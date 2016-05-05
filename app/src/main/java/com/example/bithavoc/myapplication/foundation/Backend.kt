package com.example.bithavoc.myapplication.foundation

import android.content.Context
import kotlin.reflect.*

open class Backend(broadcaster: Broadcaster, context: Context) {
    val handlers = HandlerRegistry()
    val globalState = GlobalStateBag(broadcaster, context)

    fun execute(actionPath: ActionPath, input: Any?, state: Any?) : Any? {
        var handler = handlers.getHandler(actionPath.handler) ?: throw Exception("Action ${actionPath}} cannot be performed since handler ${actionPath.handler} is missing")
        val actionDescription = describeAction(handler, actionPath)
        if(!actionDescription.canAcceptInputCheckingNullability(input)) {
            throw Exception("Action ${actionPath}} cannot be performed with input ${input} since implementation method is has not defined @${Input::class.simpleName} parameter or accepting nullable inputs")
        }
        val params = mutableMapOf<KParameter, Any?>(actionDescription.instanceParam to handler)
        if(actionDescription.shouldAddInput(input)) {
            params[actionDescription.inputParam!!] = input
        }
        if(actionDescription.shouldAddState(state)) {
            params[actionDescription.stateParam!!] = state
        }
        for (statePlaceholder in actionDescription.inGlobalStates) {
            var state = globalState.retrieve(statePlaceholder.id)
            if(state != null) {
                params[statePlaceholder.parameter] = state
                continue
            }
            if(!statePlaceholder.optional) {
                throw Exception("Action ${actionPath}} cannot be performed since non-optional global state ${statePlaceholder.id} was not found")
            }
        }
        var result: Any? = null
        val resultBag = actionDescription.methodImpl.callBy(params)
        if(resultBag != null) {
            for(property in resultBag.javaClass.kotlin.memberProperties) {
                property.annotations.map {it as? GlobalState}.filterNotNull().forEach { globalStateAnnotation ->
                    val currentValue = property.get(resultBag)
                    val globalStateId = globalStateAnnotation.toIndentifier(name=property.returnType.toString())
                    globalState.publish(globalStateId, currentValue)
                }
                property.annotations.map {it as? ActionState}.filterNotNull().forEach {
                    val returnActionState = property.get(resultBag)
                    result = returnActionState
                }
            }
        }
        return result
    }

    fun republishGlobalState(id:GlobalStateIdentifier) {
        globalState.republish(id)
    }

    private fun describeAction(handler: ActionHandler, actionPath: ActionPath) : ActionDescription {
        var actionMethod = handler.javaClass.kotlin.members.find { it.name == actionPath.action } ?: throw Exception("Action ${actionPath}} cannot be performed since implementation for action ${actionPath.action} is missing")
        var instanceParam = actionMethod.parameters.find { it.kind == KParameter.Kind.INSTANCE } ?: throw Exception("Action ${actionPath}} cannot be performed since implementation is based on a static method")
        var serviceActionAnnotation = actionMethod.annotations.find { it is ServiceAction } ?: throw Exception("Action ${actionPath}} found in $handler is missing required annotation @${ServiceAction::class.simpleName}")
        var inputParam = actionMethod.parameters.find { it.annotations.any { ann -> ann is Input } }
        var stateParam = actionMethod.parameters.find { it.annotations.any { ann -> ann is ActionState } }
        var inGlobalStates = actionMethod.parameters.filter { it.annotations.any {it is GlobalState } }.map {
            Pair(it.annotations.map { it as GlobalState }.first(),it)
        }.map {
            GlobalStateArgumentPlaceholder(id = it.first.toIndentifier((it.second.type.toString())), parameter = it.second)
        }
        return ActionDescription(actionMethod, instanceParam, inputParam, stateParam, inGlobalStates)
    }
}
