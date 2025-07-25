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
        val (sbField, appendMethod) = initializeField(pluginContext, moduleFragment)

        // Traverse and transform functions
        moduleFragment.files.forEach { file ->
            file.transform(object : IrElementTransformerVoidWithContext() {
                override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                    val body = declaration.body ?: return declaration

                    // Skip constructors and synthetic functions
                    if (declaration.name.asString() in listOf("<init>", "<clinit>")) return declaration
                    if (declaration.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE) return declaration

                    declaration.body = generateCall(DeclarationIrBuilder(pluginContext, declaration.symbol), pluginContext, body.statements, declaration.name.asString(), sbField, appendMethod)
                    return declaration
                }
            }, null)
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun initializeField(pluginContext: IrPluginContext, moduleFragment: IrModuleFragment): Pair<IrField, IrSimpleFunctionSymbol> {
        val sbClass = pluginContext.findClass("java/lang/StringBuilder")!!
        val constructor = sbClass.findConstructor(pluginContext)!!
        val appendMethod = sbClass.findFunction(pluginContext, "append(string?)")!!

        // Define global sb field
        val firstFile = moduleFragment.files.first()
        val sbField = pluginContext.createField(firstFile.symbol, "_globalSB") { constructor() }
        firstFile.declarations.add(sbField)
        sbField.parent = firstFile

        return Pair(sbField, appendMethod)
    }

    private fun generateCall(builder: DeclarationIrBuilder, pluginContext: IrPluginContext, statements: List<IrStatement>, methodName: String, sbField: IrField, appendMethod: IrSimpleFunctionSymbol): IrBlockBody {
        return builder.irBlockBody {
            enableCallDSL(pluginContext) {
                +sbField.call(appendMethod, "$methodName --> ")

                for (stmt in statements) {
                    +stmt
                }

                // Print final value in main
                if (methodName == "main") {
                    +irPrintLn(pluginContext, sbField.call("toString()"))
                }
            }
        }
    }

    /*
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun initializeField(pluginContext: IrPluginContext, moduleFragment: IrModuleFragment): Pair<IrField, IrSimpleFunctionSymbol> {
        val sbClass = pluginContext.referenceClass(ClassId.topLevel(FqName("java.lang.StringBuilder")))!!
        val sbType = sbClass.defaultType

        val sbConstructor = sbClass.constructors.single { it.owner.valueParameters.isEmpty() }
        val appendMethod = sbClass.functions.single {
            it.owner.name.asString() == "append" &&
            it.owner.valueParameters.size == 1 &&
            it.owner.valueParameters[0].type == pluginContext.irBuiltIns.stringType
        }
        val toStringFunc = sbClass.functions.single {
            it.owner.name.asString() == "toString" && it.owner.valueParameters.isEmpty()
        }

        val printlnFunc = pluginContext.referenceFunctions(
            CallableId(FqName("kotlin.io"), Name.identifier("println"))
        ).single {
            it.owner.valueParameters.size == 1 &&
            it.owner.valueParameters[0].type == pluginContext.irBuiltIns.anyNType
        }

        // Create global StringBuilder field
        val firstFile = moduleFragment.files.first()
        val sbField = pluginContext.irFactory.buildField {
            name = Name.identifier("_globalSB")
            type = sbType
            isFinal = true
            isStatic = true
        }.apply {
            initializer = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(irCall(sbConstructor))
            }
        }
        firstFile.declarations.add(sbField)
        sbField.parent = firstFile

        return Pair(sbField, appendMethod, printlnFunc, toStringFunc)
    }

    private fun generateCall(builder: DeclarationIrBuilder, pluginContext: IrPluginContext, statements: List<IrStatement>, methodName: String, sbField: IrField, appendMethod: IrSimpleFunctionSymbol, printlnFunc : IrSimpleFunctionSymbol, toStringFunc: IrSimpleFunctionSymbol): IrBlockBody {
        return builder.irBlockBody {
            +builder.irCall(appendFunc).apply {
                dispatchReceiver = builder.irGetField(null, sbField)
                putValueArgument(0, builder.irString("methodName;"))
            }

            for (stmt in statements) {
                +stmt
            }

            // Print final value in main
            if (methodName == "main") {
                +builder.irCall(printlnFunc).apply {
                putValueArgument(0, builder.irCall(toStringFunc).apply {
                    dispatchReceiver = builder.irGetField(null, sbField)
                })
            }
            }
        }
    }
     */
}