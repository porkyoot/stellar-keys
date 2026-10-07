package com.stellar.keys.config

import com.stellar.keys.atlas.KeyCategory
import com.stellar.keys.atlas.KeybindAtlasClientConfig
import com.stellar.keys.atlas.layout.KeyboardLayouts
import org.quiltmc.config.api.ReflectiveConfig
import org.quiltmc.config.api.annotations.Comment
import org.quiltmc.config.api.values.TrackedValue
import java.util.Locale

/**
 * Quilt ReflectiveConfig for Stellar Keys, implementing KeybindAtlasClientConfig.Backend.
 */
@Suppress("TooManyFunctions", "LargeClass")
class StellarKeysConfig : ReflectiveConfig(), KeybindAtlasClientConfig.Backend {
    @Comment("Default pixel scale: 1 (auto/standard) or 2 (enlarged)")
    val pixelScale: TrackedValue<Int> = value(DEFAULT_PIXEL_SCALE)

    @Comment("Keyboard layout preset (e.g. US_QWERTY, UK_QWERTY, DE_QWERTZ, FR_AZERTY)")
    val layoutPreset: TrackedValue<String> = value("US_QWERTY")

    @Comment("Enable UI animations")
    val uiAnimations: TrackedValue<Boolean> = value(true)

    @Comment("Enable hidden bindings filtering")
    val filterHiddenBindings: TrackedValue<Boolean> = value(true)

    @Comment("Enable custom categories coloring")
    val enableCategories: TrackedValue<Boolean> = value(true)

    @Comment("Enable Function keys (F1-F12) row")
    val enableFKeys: TrackedValue<Boolean> = value(false)

    @Comment("Display all keys instead of only bound ones")
    val displayAllKeys: TrackedValue<Boolean> = value(true)

    @Comment("Use alternating line colors for key connector lines")
    val alternatingLines: TrackedValue<Boolean> = value(true)

    @Comment("Panel background opacity (0.0 to 1.0)")
    val backgroundOpacity: TrackedValue<Float> = value(DEFAULT_PANEL_OPACITY)

    @Comment("Maximum unhovered key assignments shown")
    val maxAssignments: TrackedValue<Int> = value(DEFAULT_MAX_UNHOVERED)

    @Comment("Keep default pixelated Minecraft font")
    val defaultFont: TrackedValue<Boolean> = value(true)

    @Comment("Newline-separated list of hidden binding labels")
    val hiddenLabels: TrackedValue<String> = value(
        KeyboardLayouts.defaultHiddenBindingLabels().joinToString("\n"),
    )

    @Comment("Newline-separated list of preset category colors: id|name|#RRGGBBAA")
    val presetColors: TrackedValue<String> = value(defaultPresetCategoryEntries().joinToString("\n"))

    @Comment("Newline-separated list of custom categories: id|name|#RRGGBBAA|#RRGGBBAA")
    val customCategoryList: TrackedValue<String> = value(defaultCustomCategoryEntries().joinToString("\n"))

    @Comment("Key category assignments mapping (target::categoryId)")
    val categoryAssignments: TrackedValue<String> = value("")

    @Comment("Disabled mod IDs list (newline-separated)")
    val disabledModList: TrackedValue<String> = value("")

    @Comment("Disabled binding keys list (newline-separated)")
    val disabledBindingList: TrackedValue<String> = value("")

    @Comment("Color: Key assigned fill")
    val colorKeyAssignedFill: TrackedValue<Int> = value(COLOR_KEY_ASSIGNED_FILL)

    @Comment("Color: Key unused fill")
    val colorKeyUnusedFill: TrackedValue<Int> = value(COLOR_KEY_UNUSED_FILL)

    @Comment("Color: Key hidden fill")
    val colorKeyHiddenFill: TrackedValue<Int> = value(COLOR_KEY_HIDDEN_FILL)

    @Comment("Color: Key hover fill")
    val colorKeyHoverFill: TrackedValue<Int> = value(COLOR_KEY_HOVER_FILL)

    @Comment("Color: Key assigned text")
    val colorKeyAssignedText: TrackedValue<Int> = value(COLOR_KEY_ASSIGNED_TEXT)

    @Comment("Color: Key unused text")
    val colorKeyUnusedText: TrackedValue<Int> = value(COLOR_KEY_UNUSED_TEXT)

    @Comment("Color: Key hidden text")
    val colorKeyHiddenText: TrackedValue<Int> = value(COLOR_KEY_HIDDEN_TEXT)

    @Comment("Color: Key hover text")
    val colorKeyHoverText: TrackedValue<Int> = value(COLOR_KEY_HOVER_TEXT)

    @Comment("Color: Box fill")
    val colorBoxFill: TrackedValue<Int> = value(COLOR_BOX_FILL)

    @Comment("Color: Box hover fill")
    val colorBoxHoverFill: TrackedValue<Int> = value(COLOR_BOX_HOVER_FILL)

    @Comment("Color: Box border")
    val colorBoxBorder: TrackedValue<Int> = value(COLOR_BOX_BORDER)

    @Comment("Color: Box hover border")
    val colorBoxHoverBorder: TrackedValue<Int> = value(COLOR_BOX_HOVER_BORDER)

    @Comment("Color: Box text")
    val colorBoxText: TrackedValue<Int> = value(COLOR_BOX_TEXT)

    @Comment("Color: Box source text")
    val colorBoxSourceText: TrackedValue<Int> = value(COLOR_BOX_SOURCE_TEXT)

    @Comment("Color: Keyboard background fill")
    val colorKeyboardFill: TrackedValue<Int> = value(COLOR_KEYBOARD_FILL)

    @Comment("Color: Keyboard background border")
    val colorKeyboardBorder: TrackedValue<Int> = value(COLOR_KEYBOARD_BORDER)

    @Comment("Color: Connector line default")
    val colorLineDefault: TrackedValue<Int> = value(COLOR_LINE_DEFAULT)

    @Comment("Color: Connector line default alt")
    val colorLineDefaultAlt: TrackedValue<Int> = value(COLOR_LINE_DEFAULT_ALT)

    @Comment("Color: Connector line hover")
    val colorLineHover: TrackedValue<Int> = value(COLOR_LINE_HOVER)

    override fun defaultPixelScale(): Int = pixelScale.value()

    override fun setDefaultPixelScale(scale: Int) {
        pixelScale.setValue(if (scale <= 1) 1 else 2, true)
        save()
    }

    override fun keyboardLayout(): KeyboardLayouts.MainLayoutPreset {
        val str = layoutPreset.value()
        return if (!str.isNullOrBlank()) {
            runCatching {
                KeyboardLayouts.MainLayoutPreset.valueOf(str.trim().uppercase(Locale.ROOT))
            }.getOrDefault(KeyboardLayouts.MainLayoutPreset.US_QWERTY)
        } else {
            KeyboardLayouts.MainLayoutPreset.US_QWERTY
        }
    }

    override fun setKeyboardLayout(layout: KeyboardLayouts.MainLayoutPreset?) {
        val name = layout?.name ?: KeyboardLayouts.MainLayoutPreset.US_QWERTY.name
        layoutPreset.setValue(name, true)
        save()
    }

    override fun animationsEnabled(): Boolean = uiAnimations.value()

    override fun setAnimationsEnabled(enabled: Boolean) {
        uiAnimations.setValue(enabled, true)
        save()
    }

    override fun hiddenBindingsEnabled(): Boolean = filterHiddenBindings.value()

    override fun setHiddenBindingsEnabled(enabled: Boolean) {
        filterHiddenBindings.setValue(enabled, true)
        save()
    }

    override fun categoriesEnabled(): Boolean = enableCategories.value()

    override fun setCategoriesEnabled(enabled: Boolean) {
        enableCategories.setValue(enabled, true)
        save()
    }

    override fun fKeysEnabled(): Boolean = enableFKeys.value()

    override fun setFKeysEnabled(enabled: Boolean) {
        enableFKeys.setValue(enabled, true)
        save()
    }

    override fun allKeysEnabled(): Boolean = displayAllKeys.value()

    override fun setAllKeysEnabled(enabled: Boolean) {
        displayAllKeys.setValue(enabled, true)
        save()
    }

    override fun alternatingLineColorsEnabled(): Boolean = alternatingLines.value()

    override fun setAlternatingLineColorsEnabled(enabled: Boolean) {
        alternatingLines.setValue(enabled, true)
        save()
    }

    override fun panelOpacity(): Float = backgroundOpacity.value()

    override fun setPanelOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0.0f, 1.0f)
        backgroundOpacity.setValue(clamped, true)
        save()
    }

    override fun renderProfilingEnabled(): Boolean = false

    override fun maxUnhoveredAssignments(): Int = maxAssignments.value()

    override fun setMaxUnhoveredAssignments(max: Int) {
        maxAssignments.setValue(max.coerceIn(1, MAX_UNHOVERED_LIMIT), true)
        save()
    }

    override fun hiddenBindingLabels(): List<String> = decodeList(hiddenLabels.value())

    override fun setHiddenBindingLabels(labels: List<String>?) {
        hiddenLabels.setValue(encodeList(labels), true)
        save()
    }

    override fun presetCategoryColors(): List<String> = decodeList(presetColors.value())

    override fun setPresetCategoryColors(colors: List<String>?) {
        presetColors.setValue(encodeList(colors), true)
        save()
    }

    override fun customCategories(): List<String> = decodeList(customCategoryList.value())

    override fun setCustomCategories(categories: List<String>?) {
        customCategoryList.setValue(encodeList(categories), true)
        save()
    }

    override fun keyCategoryAssignments(): List<String> = decodeList(categoryAssignments.value())

    override fun setKeyCategoryAssignments(assignments: List<String>?) {
        categoryAssignments.setValue(encodeList(assignments), true)
        save()
    }

    override fun disabledMods(): List<String> = decodeList(disabledModList.value())

    override fun setDisabledMods(mods: List<String>?) {
        disabledModList.setValue(encodeList(mods), true)
        save()
    }

    override fun disabledBindings(): List<String> = decodeList(disabledBindingList.value())

    override fun setDisabledBindings(bindings: List<String>?) {
        disabledBindingList.setValue(encodeList(bindings), true)
        save()
    }

    override fun keyAssignedFill(): Int = colorKeyAssignedFill.value()
    override fun keyUnusedFill(): Int = colorKeyUnusedFill.value()
    override fun keyHiddenFill(): Int = colorKeyHiddenFill.value()
    override fun keyHoverFill(): Int = colorKeyHoverFill.value()
    override fun keyAssignedText(): Int = colorKeyAssignedText.value()
    override fun keyUnusedText(): Int = colorKeyUnusedText.value()
    override fun keyHiddenText(): Int = colorKeyHiddenText.value()
    override fun keyHoverText(): Int = colorKeyHoverText.value()
    override fun boxFill(): Int = colorBoxFill.value()
    override fun boxHoverFill(): Int = colorBoxHoverFill.value()
    override fun boxBorder(): Int = colorBoxBorder.value()
    override fun boxHoverBorder(): Int = colorBoxHoverBorder.value()
    override fun boxText(): Int = colorBoxText.value()
    override fun boxSourceText(): Int = colorBoxSourceText.value()
    override fun keyboardFill(): Int = colorKeyboardFill.value()
    override fun keyboardBorder(): Int = colorKeyboardBorder.value()
    override fun lineDefault(): Int = colorLineDefault.value()
    override fun lineDefaultAlt(): Int = colorLineDefaultAlt.value()
    override fun lineHover(): Int = colorLineHover.value()

    override fun restoreDefaults() {
        pixelScale.setValue(DEFAULT_PIXEL_SCALE, true)
        layoutPreset.setValue("US_QWERTY", true)
        uiAnimations.setValue(true, true)
        filterHiddenBindings.setValue(true, true)
        enableCategories.setValue(true, true)
        enableFKeys.setValue(false, true)
        displayAllKeys.setValue(true, true)
        alternatingLines.setValue(true, true)
        backgroundOpacity.setValue(DEFAULT_PANEL_OPACITY, true)
        maxAssignments.setValue(DEFAULT_MAX_UNHOVERED, true)
        defaultFont.setValue(true, true)
        hiddenLabels.setValue(KeyboardLayouts.defaultHiddenBindingLabels().joinToString("\n"), true)
        presetColors.setValue(defaultPresetCategoryEntries().joinToString("\n"), true)
        customCategoryList.setValue(defaultCustomCategoryEntries().joinToString("\n"), true)
        categoryAssignments.setValue("", true)
        disabledModList.setValue("", true)
        disabledBindingList.setValue("", true)
        colorKeyAssignedFill.setValue(COLOR_KEY_ASSIGNED_FILL, true)
        colorKeyUnusedFill.setValue(COLOR_KEY_UNUSED_FILL, true)
        colorKeyHiddenFill.setValue(COLOR_KEY_HIDDEN_FILL, true)
        colorKeyHoverFill.setValue(COLOR_KEY_HOVER_FILL, true)
        colorKeyAssignedText.setValue(COLOR_KEY_ASSIGNED_TEXT, true)
        colorKeyUnusedText.setValue(COLOR_KEY_UNUSED_TEXT, true)
        colorKeyHiddenText.setValue(COLOR_KEY_HIDDEN_TEXT, true)
        colorKeyHoverText.setValue(COLOR_KEY_HOVER_TEXT, true)
        colorBoxFill.setValue(COLOR_BOX_FILL, true)
        colorBoxHoverFill.setValue(COLOR_BOX_HOVER_FILL, true)
        colorBoxBorder.setValue(COLOR_BOX_BORDER, true)
        colorBoxHoverBorder.setValue(COLOR_BOX_HOVER_BORDER, true)
        colorBoxText.setValue(COLOR_BOX_TEXT, true)
        colorBoxSourceText.setValue(COLOR_BOX_SOURCE_TEXT, true)
        colorKeyboardFill.setValue(COLOR_KEYBOARD_FILL, true)
        colorKeyboardBorder.setValue(COLOR_KEYBOARD_BORDER, true)
        colorLineDefault.setValue(COLOR_LINE_DEFAULT, true)
        colorLineDefaultAlt.setValue(COLOR_LINE_DEFAULT_ALT, true)
        colorLineHover.setValue(COLOR_LINE_HOVER, true)
        save()
    }

    companion object {
        const val DEFAULT_PIXEL_SCALE: Int = 1
        const val DEFAULT_PANEL_OPACITY: Float = 0.4f
        const val DEFAULT_MAX_UNHOVERED: Int = 3
        const val MAX_UNHOVERED_LIMIT: Int = 20

        const val COLOR_KEY_ASSIGNED_FILL: Int = -16_686_644
        const val COLOR_KEY_UNUSED_FILL: Int = -13_684_426
        const val COLOR_KEY_HIDDEN_FILL: Int = -14_737_115
        const val COLOR_KEY_HOVER_FILL: Int = -10_570_753
        const val COLOR_KEY_ASSIGNED_TEXT: Int = -1
        const val COLOR_KEY_UNUSED_TEXT: Int = -6_380_888
        const val COLOR_KEY_HIDDEN_TEXT: Int = -4_868_683
        const val COLOR_KEY_HOVER_TEXT: Int = -3_090_208
        const val COLOR_BOX_FILL: Int = -12_960_446
        const val COLOR_BOX_HOVER_FILL: Int = -12_104_878
        const val COLOR_BOX_BORDER: Int = -15_000_544
        const val COLOR_BOX_HOVER_BORDER: Int = -9_262_869
        const val COLOR_BOX_TEXT: Int = -1_710_619
        const val COLOR_BOX_SOURCE_TEXT: Int = -6_314_578
        const val COLOR_KEYBOARD_FILL: Int = -15_066_080
        const val COLOR_KEYBOARD_BORDER: Int = -15_921_648
        const val COLOR_LINE_DEFAULT: Int = -7_696_490
        const val COLOR_LINE_DEFAULT_ALT: Int = -10_723_224
        const val COLOR_LINE_HOVER: Int = -7_947_009

        private const val SHIFT_24: Int = 24
        private const val SHIFT_16: Int = 16
        private const val SHIFT_8: Int = 8
        private const val MASK_FF: Int = 0xFF

        fun formatColor(argbColor: Int): String {
            val alpha = argbColor ushr SHIFT_24 and MASK_FF
            val red = argbColor ushr SHIFT_16 and MASK_FF
            val green = argbColor ushr SHIFT_8 and MASK_FF
            val blue = argbColor and MASK_FF
            return String.format(Locale.ROOT, "#%02X%02X%02X%02X", red, green, blue, alpha)
        }

        fun defaultCustomCategoryEntries(): List<String> {
            return KeyCategory.defaultCustomCategories().map { category ->
                "${category.id()}|${KeyCategory.sanitizeName(category.name())}|" +
                    "${formatColor(category.fillColor())}|${formatColor(category.textColor())}"
            }
        }

        fun defaultPresetCategoryEntries(): List<String> {
            return KeyCategory.presetColors().map { preset ->
                "${preset.id()}|${KeyCategory.sanitizeName(preset.name())}|${formatColor(preset.fillColor())}"
            }
        }

        private fun decodeList(encoded: String?): List<String> {
            if (encoded.isNullOrBlank()) return emptyList()
            val values = LinkedHashSet<String>()
            for (part in encoded.lines()) {
                val trimmed = part.trim()
                if (trimmed.isNotEmpty()) {
                    values.add(trimmed)
                }
            }
            return ArrayList(values)
        }

        private fun encodeList(values: List<String>?): String {
            if (values.isNullOrEmpty()) return ""
            val normalized = LinkedHashSet<String>()
            for (value in values) {
                val trimmed = value.trim()
                if (trimmed.isNotEmpty()) {
                    normalized.add(trimmed)
                }
            }
            return normalized.joinToString("\n")
        }
    }
}
