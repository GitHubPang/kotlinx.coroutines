/*
 * Copyright 2016-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.debug.junit5

import kotlinx.coroutines.debug.*
import kotlinx.coroutines.debug.runWithTimeoutDumpingCoroutines
import org.junit.jupiter.api.extension.*
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.*
import java.util.*

public class CoroutinesTimeoutException(public val timeoutMs: Long): Exception("test timed out ofter $timeoutMs ms")

public class CoroutinesTimeoutExtension internal constructor(
    private val enableCoroutineCreationStackTraces: Boolean = true,
    private val timeoutMs: Long? = null,
    private val cancelOnTimeout: Boolean? = null): InvocationInterceptor
{
    public constructor(timeoutMs: Long, cancelOnTimeout: Boolean = false,
                       enableCoroutineCreationStackTraces: Boolean = true):
        this(enableCoroutineCreationStackTraces, timeoutMs, cancelOnTimeout)

    public companion object {
        private val NAMESPACE: ExtensionContext.Namespace =
            ExtensionContext.Namespace.create("kotlinx", "coroutines", "debug", "junit5", "CoroutinesTimeout")

        @JvmOverloads
        public fun seconds(timeout: Int, cancelOnTimeout: Boolean = false,
                           enableCoroutineCreationStackTraces: Boolean = true): CoroutinesTimeoutExtension =
            CoroutinesTimeoutExtension(enableCoroutineCreationStackTraces, timeout.toLong() * 1000, cancelOnTimeout)
    }

    override fun <T : Any?> interceptTestClassConstructor(
        invocation: InvocationInterceptor.Invocation<T>,
        invocationContext: ReflectiveInvocationContext<Constructor<T>>,
        extensionContext: ExtensionContext
    ): T {
        val store: ExtensionContext.Store = extensionContext.getStore(NAMESPACE)
        if (store["debugProbes"] == null) {
            /** no [DebugProbes] uninstaller is present, so this must be the first test that this instance of
             * [CoroutinesTimeoutExtension] runs. Install the [DebugProbes]. */
            DebugProbes.enableCreationStackTraces = enableCoroutineCreationStackTraces
            DebugProbes.install()
            /** put a fake resource into this extensions's store so that JUnit cleans it up, uninstalling the
             * [DebugProbes] after this extension instance is no longer needed. **/
            store.put("debugProbes", ExtensionContext.Store.CloseableResource { DebugProbes.uninstall() })
        }
        return invocation.proceed()
    }

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptNormalMethod(invocation, invocationContext, extensionContext)
    }

    override fun interceptAfterAllMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptLifecycleMethod(invocation, invocationContext, extensionContext)
    }

    override fun interceptAfterEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptLifecycleMethod(invocation, invocationContext, extensionContext)
    }

    override fun interceptBeforeAllMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptLifecycleMethod(invocation, invocationContext, extensionContext)
    }

    override fun interceptBeforeEachMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptLifecycleMethod(invocation, invocationContext, extensionContext)
    }

    override fun <T : Any?> interceptTestFactoryMethod(
        invocation: InvocationInterceptor.Invocation<T>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ): T = interceptNormalMethod(invocation, invocationContext, extensionContext)

    override fun interceptTestTemplateMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        interceptNormalMethod(invocation, invocationContext, extensionContext)
    }

    private fun<T> Class<T>.coroutinesTimeoutAnnotation(): Optional<CoroutinesTimeout> =
        AnnotationSupport.findAnnotation(this, CoroutinesTimeout::class.java).or {
            enclosingClass?.coroutinesTimeoutAnnotation() ?: Optional.empty()
        }

    private fun <T: Any?> interceptMethod(
        useClassAnnotation: Boolean,
        invocation: InvocationInterceptor.Invocation<T>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ): T {
        val testAnnotationOptional =
            AnnotationSupport.findAnnotation(invocationContext.executable, CoroutinesTimeout::class.java)
        val classAnnotationOptional = extensionContext.testClass.flatMap { it.coroutinesTimeoutAnnotation() }
        if (timeoutMs != null && cancelOnTimeout != null) {
            // this means we @RegisterExtension was used in order to register this extension.
            if (testAnnotationOptional.isPresent || classAnnotationOptional.isPresent) {
                /* Using annotations creates a separate instance of the extension, which composes in a strange way: both
                timeouts are applied. This is at odds with the concept that method-level annotations override the outer
                rules and may lead to unexpected outcomes, so we prohibit this. */
                throw UnsupportedOperationException("Using CoroutinesTimeout along with instance field-registered CoroutinesTimeout is prohibited; please use either @RegisterExtension or @CoroutinesTimeout, but not both")
            }
            return interceptInvocation(invocation, invocationContext.executable.name, timeoutMs, cancelOnTimeout)
        }
        /* The extension was registered via an annotation; check that we succeeded in finding the annotation that led to
        the extension being registered and taking its parameters. */
        if (testAnnotationOptional.isEmpty && classAnnotationOptional.isEmpty) {
            throw UnsupportedOperationException("Timeout was registered with a CoroutinesTimeout annotation, but we were unable to find it. Please report this.")
        }
        return when {
            testAnnotationOptional.isPresent -> {
                val annotation = testAnnotationOptional.get()
                interceptInvocation(invocation, invocationContext.executable.name, annotation.testTimeoutMs,
                    annotation.cancelOnTimeout)
            }
            useClassAnnotation && classAnnotationOptional.isPresent -> {
                val annotation = classAnnotationOptional.get()
                interceptInvocation(invocation, invocationContext.executable.name, annotation.testTimeoutMs,
                    annotation.cancelOnTimeout)
            }
            else -> {
                invocation.proceed()
            }
        }
    }

    private fun<T> interceptNormalMethod(
        invocation: InvocationInterceptor.Invocation<T>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ): T = interceptMethod(true, invocation, invocationContext, extensionContext)

    private fun interceptLifecycleMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) = interceptMethod(false, invocation, invocationContext, extensionContext)

    private fun <T : Any?> interceptInvocation(
        invocation: InvocationInterceptor.Invocation<T>,
        methodName: String,
        testTimeoutMs: Long,
        cancelOnTimeout: Boolean
    ): T =
        runWithTimeoutDumpingCoroutines(methodName, testTimeoutMs, cancelOnTimeout,
            { CoroutinesTimeoutException(testTimeoutMs) }, { invocation.proceed() })
}