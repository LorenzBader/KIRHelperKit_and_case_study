import at.ssw.compilerplugin.PerfMeasureComponentRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KPerfMeasureCompilerPluginTest {
    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `plugin success`() {
        val result = compile(
            SourceFile.kotlin(
                "main.kt",
                """
                    fun main() {
                      greet()
                      val v1 = 5
                      val addRes = v1 + 17
                      val threeDots = ".".repeat(3)
                      output(threeDots + " " + addRes + " " + threeDots)
                      println(debug())
                      val str = " Test!"
                      output(str)
                      a()
                      println("End of main")
                    }

                    fun debug() = "Hello, World!"

                    fun output(str: String) {
                      println(str)
                    }

                    fun a() {
                        println("a is a unit method and prints this")
                    }

                    fun greet(greeting: String = "Hello", name: String = "World") {
                      println("⇢ greet(greeting=${'$'}greeting, name=${'$'}name)")
                    }
                    """
            )
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        result.main()
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFiles: List<SourceFile>,
        compilerPluginRegistrar: CompilerPluginRegistrar = PerfMeasureComponentRegistrar(),
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            // To have access to kotlinx.io
            inheritClassPath = true
            sources = sourceFiles
            compilerPluginRegistrars = listOf(compilerPluginRegistrar)
            // commandLineProcessors = ...
            // inheritClassPath = true
        }.compile()
    }

    @OptIn(ExperimentalCompilerApi::class)
    fun compile(
        sourceFile: SourceFile,
        compilerPluginRegistrar: CompilerPluginRegistrar = PerfMeasureComponentRegistrar(),
    ) = compile(listOf(sourceFile), compilerPluginRegistrar)
}

@OptIn(ExperimentalCompilerApi::class)
private fun JvmCompilationResult.main(packageName: String = "") {
    val className = if (packageName.isNotEmpty()) "$packageName.MainKt" else "MainKt"
    val kClazz = classLoader.loadClass(className)
    val main = kClazz.declaredMethods.single { it.name.endsWith("main") && it.parameterCount == 0 }
    main.invoke(null)
}
