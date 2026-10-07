package com.stellar.keys.config

import com.stellar.core.config.ConfigManager
import com.stellar.keys.StellarKeysMod
import com.stellar.keys.atlas.layout.KeyboardLayouts
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/**
 * Factory for creating the Cloth Config GUI screen bound to StellarKeysConfig.
 */
object KeysClothConfigScreen {
    private const val PERCENT_SCALE = 100f
    private const val MAX_PERCENT = 100
    private const val DEFAULT_OPACITY_PERCENT = 40
    private const val MAX_UNHOVERED_ASSIGNMENTS = 20
    private const val DEFAULT_UNHOVERED_ASSIGNMENTS = 3

    fun create(parent: Screen?): Screen {
        val config = ConfigManager.get<StellarKeysConfig>(StellarKeysMod.MOD_ID, "main")
            ?: ConfigManager.register(StellarKeysMod.MOD_ID, "main", StellarKeysConfig::class.java)

        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.literal("Stellar Keys Config"))

        builder.setSavingRunnable {
            config.save()
        }

        val entryBuilder = builder.entryBuilder()
        buildGeneralCategory(builder, entryBuilder, config)
        buildAppearanceCategory(builder, entryBuilder, config)

        return builder.build()
    }

    private fun buildGeneralCategory(builder: ConfigBuilder, entries: ConfigEntryBuilder, config: StellarKeysConfig) {
        val category = builder.getOrCreateCategory(Component.literal("General"))
        addLayoutEntries(category, entries, config)
        addToggleEntries(category, entries, config)
    }

    private fun addLayoutEntries(category: ConfigCategory, entries: ConfigEntryBuilder, config: StellarKeysConfig) {
        val layoutOptions = KeyboardLayouts.MainLayoutPreset.entries.map { it.name }.toTypedArray()
        val currentLayout = config.layoutPreset.value().uppercase().let {
            if (it in layoutOptions) it else "US_QWERTY"
        }

        val layoutEntry = entries
            .startSelector(Component.literal("Keyboard Layout"), layoutOptions, currentLayout)
            .setDefaultValue("US_QWERTY")
            .setTooltip(Component.literal("Layout preset for keyboard rendering."))
            .setSaveConsumer { value -> config.layoutPreset.setValue(value, true) }
            .build()

        val scaleOptions = arrayOf("1x (Standard)", "2x (Enlarged)")
        val currentScale = if (config.pixelScale.value() == 2) scaleOptions[1] else scaleOptions[0]
        val scaleEntry = entries
            .startSelector(Component.literal("Pixel Scale"), scaleOptions, currentScale)
            .setDefaultValue(scaleOptions[0])
            .setTooltip(Component.literal("Render size scale of the keyboard atlas."))
            .setSaveConsumer { value ->
                config.pixelScale.setValue(if (value == scaleOptions[1]) 2 else 1, true)
            }
            .build()

        category.addEntry(layoutEntry)
        category.addEntry(scaleEntry)
    }

    private fun addToggleEntries(category: ConfigCategory, entries: ConfigEntryBuilder, config: StellarKeysConfig) {
        val animEntry = entries
            .startBooleanToggle(Component.literal("UI Animations"), config.uiAnimations.value())
            .setDefaultValue(true)
            .setTooltip(Component.literal("Enable smooth UI transitions and fade effects."))
            .setSaveConsumer { value -> config.uiAnimations.setValue(value, true) }
            .build()

        val fKeysEntry = entries
            .startBooleanToggle(Component.literal("Show F-Keys Row"), config.enableFKeys.value())
            .setDefaultValue(false)
            .setTooltip(Component.literal("Display top row of function keys (F1-F12)."))
            .setSaveConsumer { value -> config.enableFKeys.setValue(value, true) }
            .build()

        val allKeysEntry = entries
            .startBooleanToggle(Component.literal("Display All Keys"), config.displayAllKeys.value())
            .setDefaultValue(true)
            .setTooltip(Component.literal("Display all keyboard keys including unbound keys."))
            .setSaveConsumer { value -> config.displayAllKeys.setValue(value, true) }
            .build()

        category.addEntry(animEntry)
        category.addEntry(fKeysEntry)
        category.addEntry(allKeysEntry)
    }

    private fun buildAppearanceCategory(
        builder: ConfigBuilder,
        entries: ConfigEntryBuilder,
        config: StellarKeysConfig,
    ) {
        val category = builder.getOrCreateCategory(Component.literal("Appearance & Visuals"))
        addFilterEntries(category, entries, config)
        addSliderEntries(category, entries, config)
    }

    private fun addFilterEntries(category: ConfigCategory, entries: ConfigEntryBuilder, config: StellarKeysConfig) {
        val categoriesEntry = entries
            .startBooleanToggle(Component.literal("Enable Categories"), config.enableCategories.value())
            .setDefaultValue(true)
            .setTooltip(Component.literal("Color code key bindings by category tags."))
            .setSaveConsumer { value -> config.enableCategories.setValue(value, true) }
            .build()

        val hiddenBindingsEntry = entries
            .startBooleanToggle(Component.literal("Filter Hidden Bindings"), config.filterHiddenBindings.value())
            .setDefaultValue(true)
            .setTooltip(Component.literal("Hide non-standard / obscure bindings from main layout."))
            .setSaveConsumer { value -> config.filterHiddenBindings.setValue(value, true) }
            .build()

        val alternatingLinesEntry = entries
            .startBooleanToggle(Component.literal("Alternating Line Colors"), config.alternatingLines.value())
            .setDefaultValue(true)
            .setTooltip(Component.literal("Use alternating contrast colors for connector lines."))
            .setSaveConsumer { value -> config.alternatingLines.setValue(value, true) }
            .build()

        val fontEntry = entries
            .startBooleanToggle(Component.literal("Default Minecraft Font"), config.defaultFont.value())
            .setDefaultValue(true)
            .setTooltip(Component.literal("Keep standard pixelated Minecraft font for all texts and dialogs."))
            .setSaveConsumer { value -> config.defaultFont.setValue(value, true) }
            .build()

        category.addEntry(categoriesEntry)
        category.addEntry(hiddenBindingsEntry)
        category.addEntry(alternatingLinesEntry)
        category.addEntry(fontEntry)
    }

    private fun addSliderEntries(category: ConfigCategory, entries: ConfigEntryBuilder, config: StellarKeysConfig) {
        val opacityPercent = (config.backgroundOpacity.value() * PERCENT_SCALE).toInt()
        val opacityEntry = entries
            .startIntSlider(
                Component.literal("Background Opacity (%)"),
                opacityPercent,
                0,
                MAX_PERCENT,
            )
            .setDefaultValue(DEFAULT_OPACITY_PERCENT)
            .setTextGetter { value -> Component.literal("$value%") }
            .setTooltip(Component.literal("Opacity percentage of the screen background overlay."))
            .setSaveConsumer { value -> config.backgroundOpacity.setValue(value / PERCENT_SCALE, true) }
            .build()

        val maxAssignmentsEntry = entries
            .startIntSlider(
                Component.literal("Max Unhovered Assignments"),
                config.maxAssignments.value(),
                1,
                MAX_UNHOVERED_ASSIGNMENTS,
            )
            .setDefaultValue(DEFAULT_UNHOVERED_ASSIGNMENTS)
            .setTooltip(Component.literal("Maximum number of assignments shown per key when unhovered."))
            .setSaveConsumer { value -> config.maxAssignments.setValue(value, true) }
            .build()

        category.addEntry(opacityEntry)
        category.addEntry(maxAssignmentsEntry)
    }
}
