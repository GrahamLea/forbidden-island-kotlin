package com.grahamlea.forbiddenisland

import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.*
import org.junit.platform.commons.util.AnnotationUtils.isAnnotated
import org.junit.platform.commons.util.Preconditions
import java.lang.reflect.Method
import java.util.stream.Stream

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@TestTemplate
@ExtendWith(RunForEachAdventurerExtension::class)
annotation class RunForEachAdventurer()

internal class RunForEachAdventurerExtension : TestTemplateInvocationContextProvider {

    override fun supportsTestTemplate(context: ExtensionContext): Boolean {
        if (!context.testMethod.isPresent) {
            return false
        }

        val testMethod = context.testMethod.get()
        if (!isAnnotated(testMethod, RunForEachAdventurer::class.java)) {
            return false
        }

        Preconditions.condition(hasValidSignature(testMethod)) {
            "@RunForEachAdventurer method [${testMethod.toGenericString()}] " +
            "must have between one and six parameters of type Adventurer"
        }

        return true
    }

    private fun hasValidSignature(testMethod: Method) =
        testMethod.parameterTypes.let { types ->
            !types.isEmpty() && !types.any { it != Adventurer::class.java }
        }

    override fun provideTestTemplateInvocationContexts(context: ExtensionContext): Stream<TestTemplateInvocationContext> =
        Stream.of(*Adventurer.values()).map { PlayerTestTemplateInvocationContext(it) }
}

internal class PlayerTestTemplateInvocationContext(val player: Adventurer) : TestTemplateInvocationContext, ParameterResolver {

    override fun getDisplayName(invocationIndex: Int) = player.toString()

    override fun getAdditionalExtensions(): List<Extension> = listOf(this)

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext) =
        parameterContext.parameter.type == Adventurer::class.java && parameterContext.index < 6

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        return if (parameterContext.index == 0) player
        else (Adventurer.values().toList() - player)[parameterContext.index - 1]
    }

}
