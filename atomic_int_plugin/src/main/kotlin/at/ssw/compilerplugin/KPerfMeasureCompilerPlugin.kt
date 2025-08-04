package at.ssw.compilerplugin

import at.ssw.compilerplugin.ExampleConfigurationKeys.KEY_ENABLED
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import at.ssw.helpers.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody

object ExampleConfigurationKeys {
    val KEY_ENABLED: CompilerConfigurationKey<Boolean> = CompilerConfigurationKey.create("enabled")
}

@OptIn(ExperimentalCompilerApi::class)
class KPerfMeasureCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = "k-perf-LB-case-study"
    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            "enabled",
            "<true|false>",
            "whether plugin is enabled"
        )
    )

    init {
        println("k-perf-LB-case-study - init")
    }

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        println("KPerfMeasureCommandLineProcessor - processOption ($option, $value)")
        when (option.optionName) {
            "enabled" -> configuration.put(KEY_ENABLED, value.toBoolean())

            else -> throw CliOptionProcessingException("KPerfMeasureCommandLineProcessor.processOption encountered unknown CLI compiler plugin option: ${option.optionName}")
        }
    }
}

@OptIn(ExperimentalCompilerApi::class)
class PerfMeasureComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean = true

    init {
        println("k-perf-LB-case-study - init")
    }

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }

        IrGenerationExtension.registerExtension(PerfMeasureExtension2())
    }
}

/*
Backend plugin
 */
class PerfMeasureExtension2(
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val (counterField, incrementMethod) = initializeField(pluginContext, moduleFragment)

        // Traverse and transform functions
        moduleFragment.files.forEach { file ->
            file.transform(object : IrElementTransformerVoidWithContext() {
                override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                    val body = declaration.body ?: return declaration

                    // Skip constructors and synthetic functions
                    if (declaration.name.asString() in listOf("<init>", "<clinit>")) return declaration
                    if (declaration.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE) return declaration

                    declaration.body = buildNewMethodBody(DeclarationIrBuilder(pluginContext, declaration.symbol), pluginContext, body.statements, declaration.name.asString(), counterField, incrementMethod)
                    return declaration
                }
            }, null)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun initializeField(pluginContext: IrPluginContext, moduleFragment: IrModuleFragment): Pair<IrField, IrSimpleFunctionSymbol> {
        //TODO: search for the java.util.concurrent.atomic.AtomicInteger class and its addAndGet method
        //TODO: create a field of type AtomicInteger and return the field and the method symbol
    }

    private fun buildNewMethodBody(builder: DeclarationIrBuilder, pluginContext: IrPluginContext, statements: List<IrStatement>, methodName: String, counterField: IrField, incrementMethod: IrSimpleFunctionSymbol): IrBlockBody {
        return builder.irBlockBody {
            //TODO: insert current statements + call to increment the counter in the new method body
        }
    }
}