package com.stellar.keys.atlas.render;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

import com.stellar.keys.atlas.AtlasText;
import com.stellar.keys.atlas.KeyBindingAccess;
import com.stellar.keys.atlas.KeyBindingModifier;
import com.stellar.keys.atlas.KeyCategory;
import com.stellar.keys.atlas.KeyVisualStyle;
import com.stellar.keys.atlas.KeybindAtlasClientConfig;
import com.stellar.keys.atlas.box.BoxDimensions;
import com.stellar.keys.atlas.box.BoxPosition;
import com.stellar.keys.atlas.box.BoxTextLayout;
import com.stellar.keys.atlas.box.BoxTextLayoutFactory;
import com.stellar.keys.atlas.box.BoxTextRow;
import com.stellar.keys.atlas.box.KeyboardBoxLayoutEngine;
import com.stellar.keys.atlas.connector.ConnectorGeometry;
import com.stellar.keys.atlas.connector.ConnectorRouteBuilder;
import com.stellar.keys.atlas.edit.CategoryNameDialog;
import com.stellar.keys.atlas.edit.ConfirmationDialog;
import com.stellar.keys.atlas.edit.EditModeOverlay;
import com.stellar.keys.atlas.edit.EditModeState;
import com.stellar.keys.atlas.edit.KeyBindingEditDialog;
import com.stellar.keys.atlas.edit.KeyCategoryPanel;
import com.stellar.keys.atlas.edit.UnassignedBindingsPanel;
import com.stellar.keys.atlas.layout.KeyAssignmentInfo;
import com.stellar.keys.atlas.layout.KeyboardBindingCache;
import com.stellar.keys.atlas.layout.KeyboardKey;
import com.stellar.keys.atlas.layout.KeyboardLayout;
import com.stellar.keys.atlas.layout.KeyboardLayouts;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class KeyboardScreen extends Screen {
   private static final float STATIC_BOX_SETTLE_MS = 570.0F;
   private static final float HOVER_TRANSITION_BASE_MS = 105.0F;
   private static final float HOVER_TRANSITION_PER_PIXEL_MS = 0.9F;
   private static final float HOVER_TRANSITION_MAX_MS = 220.0F;
   private static final int BINDING_REFRESH_BATCH_SIZE = 24;
   private static final int HUD_MARGIN = 6;
   private static final int DEBUG_PANEL_PAD_X = 4;
   private static final int DEBUG_PANEL_PAD_Y = 4;
   private static final int DEBUG_PANEL_ROW_GAP = 2;
   private static final float DEBUG_PANEL_LABEL_SCALE = 0.8F;
   private static final float DEBUG_PATH_LABEL_SCALE = 0.55F;
   private static final DateTimeFormatter DEBUG_LOG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int DEBUG_OVERLAY_TEXT_COLOR = 12436428;
   private static final float CATEGORY_INFO_TEXT_SCALE = 0.75F;
   private static final String UNUSED_LEGEND_CATEGORY_ID = "__unused__";
   private static final float LEGEND_FILTER_FADE_ALPHA = 0.12F;
   private static final float LEGEND_FILTER_TRANSITION_MS = 140.0F;
   private final Screen parentScreen;
   private static final Component HIDDEN_KEYS_STATUS_MESSAGE = AtlasText.translatable("ui.status.hidden_keys_toggle");
   private static final float TRANSITION_BOX_FADE_MS = 150.0F;
   private static final float TRANSITION_SLIDE_MS = 350.0F;
   private static final int RENDERER_PREWARM_BATCH_SIZE = 12;
   private static final int PREPARED_GEOMETRY_CACHE_SIZE = 24;
   private static final int PREPARED_GEOMETRY_REUSE_CACHE_SIZE = 48;
   private static final ExecutorService GEOMETRY_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "KeybindAtlas-Geometry");
      thread.setDaemon(true);
      return thread;
   });
   private static KeyboardRenderer sharedRenderer = null;
   private static final Map<KeyboardScreen.PreparedGeometryKey, KeyboardScreen.PreparedGeometry> sharedPreparedGeometryCache = new LinkedHashMap<KeyboardScreen.PreparedGeometryKey, KeyboardScreen.PreparedGeometry>(
      16, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<KeyboardScreen.PreparedGeometryKey, KeyboardScreen.PreparedGeometry> eldest) {
         return this.size() > 24;
      }
   };
   private static final Map<KeyboardScreen.PreparedGeometryKey, CompletableFuture<KeyboardScreen.PreparedGeometry>> sharedPreparedGeometryFutures = new LinkedHashMap<>();
   private static final Map<KeyboardScreen.PreparedGeometryReuseKey, KeyboardScreen.PreparedGeometrySnapshot> sharedPreparedGeometryReuseCache = new LinkedHashMap<KeyboardScreen.PreparedGeometryReuseKey, KeyboardScreen.PreparedGeometrySnapshot>(
      32, 0.75F, true
   ) {
      @Override
      protected boolean removeEldestEntry(Entry<KeyboardScreen.PreparedGeometryReuseKey, KeyboardScreen.PreparedGeometrySnapshot> eldest) {
         return this.size() > 48;
      }
   };
   private static final Set<KeyboardScreen.PreparedGeometryKey> sharedRendererWarmKeys = new LinkedHashSet<>();
   private static final AtomicInteger geometryGeneration = new AtomicInteger();
   private final KeyboardBindingCache bindingCache = KeyboardBindingCache.shared();
   private List<KeyboardKey> allKeys = List.of();
   private boolean isDebugMode = false;
   private int debugIndex = -1;
   private int scaleIndex;
   private Map<KeyboardKey, BoxPosition> cachedBoxPositions = new LinkedHashMap<>();
   private List<ConnectorGeometry> cachedConnectorGeometry = List.of();
   private Map<KeyboardKey, BoxPosition> cachedAuxBoxPositions = new LinkedHashMap<>();
   private List<ConnectorGeometry> cachedAuxConnectorGeometry = List.of();
   private boolean auxBindingLayoutCached = false;
   private int keyboardMinX;
   private int keyboardMaxX;
   private int keyboardMinY;
   private int keyboardMaxY;
   private int keyboardHousingX;
   private int keyboardHousingY;
   private int keyboardHousingWidth;
   private int keyboardHousingHeight;
   private int hoveredKeyX = -1;
   private int hoveredKeyY = -1;
   private KeyboardKey hoveredBoxKey = null;
   private KeyboardScreen.DynamicBoxEntry deferredHoveredEntry = null;
   private final Map<Integer, Long> boxAnimStart = new HashMap<>();
   private final Map<Integer, Float> hoverProgressByKey = new HashMap<>();
   private final Map<String, Float> legendFilterAlphaByCategory = new HashMap<>();
   private long lastHoverAnimationNanos = -1L;
   private long lastLegendFilterAnimationNanos = -1L;
   private KeyboardRenderer renderer;
   private BoxTextLayoutFactory cachedLayoutFactory;
   private KeyboardDebugOverlay cachedDebugOverlay;
   private KeyboardLegendOverlay cachedLegendOverlay;
   private KeyboardScreen.StaticLayerPlan staticLayerPlan = KeyboardScreen.StaticLayerPlan.empty();
   private List<Integer> staticSettledBoxKeys = List.of();
   private boolean settingsOpen = false;
   private boolean layoutMenuOpen = false;
   private boolean restorePending = false;
   private boolean settingsDebugExpanded = false;
   private boolean modFilterOpen = false;
   private List<String> modSources = List.of();
   private Map<String, Boolean> modEnabled = new LinkedHashMap<>();
   private int modPanelScroll = 0;
   private Map<String, Boolean> modExpanded = new LinkedHashMap<>();
   private Map<String, Float> modExpandProgress = new LinkedHashMap<>();
   private Map<String, Map<String, Boolean>> bindingEnabled = new LinkedHashMap<>();
   private Map<String, List<String>> bindingsByMod = new LinkedHashMap<>();
   private boolean assignmentFiltersActive = false;
   private final Map<KeyboardScreen.FilteredLayoutKey, BoxTextLayout> filteredLayoutCache = new HashMap<>();
   private final Map<Integer, Boolean> visibleAssignmentsByKey = new HashMap<>();
   private int mainVisibleBindingBoxCount = -1;
   private int auxVisibleBindingBoxCount = -1;
   private List<ConnectorGeometry> frameVisibleGeometry = List.of();
   private Set<Integer> cachedVisibleAnimationKeys = null;
   private boolean auxKeysActive = false;
   private KeyboardLayout auxLayout = null;
   private long auxActivatedNanos = -1L;
   private KeyboardScreen.StaticLayerPlan auxStaticLayerPlan = KeyboardScreen.StaticLayerPlan.empty();
   private List<Integer> auxStaticSettledBoxKeys = List.of();
   private boolean transitionActive = false;
   private long transitionStartNanos = -1L;
   private boolean transitionToAux = false;
   private float settingsPanelProgress = 0.0F;
   private float modFilterPanelProgress = 0.0F;
   private float categoryModeHudProgress = 0.0F;
   private long lastPanelAnimNanos = -1L;
   private static final float PANEL_ANIM_MS_PER_PX = 1.5F;
   private static final float SETTINGS_PANEL_H_EST = 176.0F;
   private static final float MOD_FILTER_PANEL_H_EST = 100.0F;
   private static final float UNASSIGNED_PANEL_H_EST = 118.0F;
   private static final float CATEGORY_PANEL_H_EST = 154.0F;
   private static final float CATEGORY_MODE_HUD_EST = 84.0F;
   private static final float EDIT_TOOLBAR_H_EST = 100.0F;
   private static final float MOD_EXPAND_H_EST = 60.0F;
   private final HoverTooltipState auxToggleTooltipState = new HoverTooltipState();
   private Component statusMessage = null;
   private long statusMessageExpiry = 0L;
   private long statusMessageStartMs = 0L;
   private boolean hiddenKeysModeActive = false;
   private boolean categoryInfoVisible = false;
   private String selectedCategoryId = "assigned";
   private String activeLegendHoverCategoryId = "";
   private boolean bindingCacheRefreshInProgress = false;
   private int bindingCacheRefreshProcessedMappings = 0;
   private int bindingCacheRefreshTotalMappings = 0;
   private boolean geometryRefreshInProgress = false;
   private long geometryRefreshStartMs = 0L;
   private boolean rendererPrewarmInProgress = false;
   private int rendererPrewarmCompletedSteps = 0;
   private int rendererPrewarmTotalSteps = 0;
   private boolean bindingLayoutDirty = true;
   private int appliedBindingCacheSignature = Integer.MIN_VALUE;
   private KeyboardScreen.PreparedGeometryKey activePreparedGeometryKey = null;
   private KeyboardScreen.RendererPrewarmState rendererPrewarmState = null;
   private int prefetchFilterIndex = 0;
   private int prefetchFilterStateHash = Integer.MIN_VALUE;
   private KeyBindingModifier lockedLayer = KeyBindingModifier.NONE;
   private KeyBindingModifier currentLayer = KeyBindingModifier.NONE;
   private static final int LAYERS_PANEL_X = 6;
   private static final int LAYERS_PANEL_Y = 28;
   private static final int LAYERS_PANEL_W = 68;
   private static final int LAYERS_PANEL_PAD = 4;
   private static final int LAYERS_HEADER_H = 12;
   private static final int LAYERS_ROW_H = 14;
   private static final int LAYERS_ROW_GAP = 2;
   private static final KeyBindingModifier[] DISPLAYED_LAYERS = {
      KeyBindingModifier.NONE,
      KeyBindingModifier.F3,
      KeyBindingModifier.CONTROL,
      KeyBindingModifier.SHIFT,
      KeyBindingModifier.ALT
   };
   private final EditModeState editModeState = new EditModeState();
   private EditModeOverlay cachedEditModeOverlay;
   private KeyCategoryPanel categoryPanel;
   private CategoryNameDialog categoryNameDialog;
   private KeyBindingEditDialog editDialog;
   private UnassignedBindingsPanel unassignedPanel;
   private Map<String, List<EditModeState.UnassignedEntry>> unassignedByMod = new LinkedHashMap<>();
   private int editPenHoveredIndex = -1;
   private String editDialogTranslationKey = null;
   private ConfirmationDialog confirmDialog;
   private KeyboardScreen.PendingConfirmAction pendingConfirmAction = KeyboardScreen.PendingConfirmAction.NONE;
   private String pendingDeleteCategoryId = "";
   private int profileFrameCount = 0;
   private long profileAccumBlur = 0L;
   private long profileAccumBase = 0L;
   private long profileAccumConnectors = 0L;
   private long profileAccumCaptureBlur = 0L;
   private long profileAccumBoxes = 0L;
   private long profileAccumDynamic = 0L;
   private long profileAccumOverlays = 0L;
   private long profileAccumTotal = 0L;
   private long profileAccumDebugOverlay = 0L;
   private long profileAccumLegend = 0L;
   private long profileAccumEditOverlay = 0L;
   private long profileAccumSuperRender = 0L;
   private long profileAccumStatusMsg = 0L;

   public KeyboardScreen() {
      this(null);
   }

   public KeyboardScreen(Screen parentScreen) {
      super(AtlasText.translatable("ui.screen.title"));
      this.parentScreen = parentScreen;
      this.scaleIndex = this.defaultCornerStyle();
   }

   private int pixelScale() {
      return this.scaleIndex + 1;
   }

   private float responsiveScale() {
      if (this.width <= 0 || this.height <= 0) {
         return 1.0F;
      }
      float scaleW = (float) this.width / 854.0F;
      float scaleH = (float) this.height / 480.0F;
      float scale = Math.min(scaleW, scaleH);
      return Math.max(0.20F, Math.min(8.0F, scale));
   }

   private int virtualWidth() {
      if (this.width <= 0) {
         return 854;
      }
      return Math.max(1, Math.round((float) this.width / this.responsiveScale()));
   }

   private int virtualHeight() {
      if (this.height <= 0) {
         return 480;
      }
      return Math.max(1, Math.round((float) this.height / this.responsiveScale()));
   }

   private int defaultCornerStyle() {
      return Math.max(0, KeybindAtlasClientConfig.defaultPixelScale() - 1);
   }

   private KeyboardLayouts.MainLayoutPreset keyboardLayoutPreset() {
      return KeybindAtlasClientConfig.keyboardLayout();
   }

   private void applyKeyboardLayoutPreset(KeyboardLayouts.MainLayoutPreset preset) {
      KeyboardLayouts.MainLayoutPreset nextLayout = preset == null ? KeyboardLayouts.MainLayoutPreset.US_QWERTY : preset.selectableEquivalent();
      if (this.keyboardLayoutPreset() != nextLayout) {
         KeybindAtlasClientConfig.setKeyboardLayout(nextLayout);
         this.markBindingLayoutDirty();
         this.disposeRenderer();
         this.init();
         this.showStatusMessage(AtlasText.translatable("ui.status.layout_changed", nextLayout.displayName()));
      }
   }

   private KeyboardLayout buildMainLayout() {
      return KeyboardLayouts.buildCenteredMainLayout(this.virtualWidth(), this.virtualHeight(), this.pixelScale(), this.fKeysEnabled(), this.keyboardLayoutPreset());
   }

   private boolean animationsEnabled() {
      return KeybindAtlasClientConfig.animationsEnabled();
   }

   private boolean fKeysEnabled() {
      return KeybindAtlasClientConfig.fKeysEnabled();
   }

   private boolean hiddenBindingsEnabled() {
      return KeybindAtlasClientConfig.hiddenBindingsEnabled();
   }

   private boolean categoriesEnabled() {
      return KeybindAtlasClientConfig.categoriesEnabled();
   }

   private String normalizeSelectedCategoryId(String categoryId) {
      if (categoryId == null || categoryId.isBlank() || "assigned".equals(categoryId)) {
         return "assigned";
      } else if ("hidden".equals(categoryId)) {
         return "hidden";
      } else if (!this.categoriesEnabled()) {
         return "assigned";
      } else {
         KeyCategory customCategory = KeybindAtlasClientConfig.findCustomCategory(categoryId);
         return customCategory != null ? customCategory.id() : "assigned";
      }
   }

   private void selectCategory(String categoryId) {
      this.selectedCategoryId = this.normalizeSelectedCategoryId(categoryId);
   }

   private KeyCategory selectedCategory() {
      this.selectedCategoryId = this.normalizeSelectedCategoryId(this.selectedCategoryId);
      if ("hidden".equals(this.selectedCategoryId)) {
         return KeyCategory.hiddenCategory();
      } else if ("assigned".equals(this.selectedCategoryId)) {
         return KeyCategory.assignedCategory();
      } else {
         KeyCategory customCategory = KeybindAtlasClientConfig.findCustomCategory(this.selectedCategoryId);
         return customCategory != null ? customCategory : KeyCategory.assignedCategory();
      }
   }

   private List<KeyCategory> categoryPanelEntries() {
      return !this.categoriesEnabled() ? List.of(KeyCategory.assignedCategory(), KeyCategory.hiddenCategory()) : KeybindAtlasClientConfig.availableCategories();
   }

   private Map<Integer, String> usedCategoryColorOwners() {
      return this.usedCategoryColorOwners("");
   }

   private Map<Integer, String> usedCategoryColorOwners(String excludedCategoryId) {
      Map<Integer, String> owners = new LinkedHashMap<>();

      for (KeyCategory category : KeybindAtlasClientConfig.customCategories()) {
         if (!category.id().equals(excludedCategoryId)) {
            owners.put(category.fillColor(), category.name());
         }
      }

      return owners;
   }

   private Map<Integer, String> categoryDialogUsedColorOwners() {
      return this.categoryNameDialog == null ? this.usedCategoryColorOwners() : this.usedCategoryColorOwners(this.categoryNameDialog.editingCategoryId());
   }

   private int nextAvailableCategoryColor() {
      for (KeyCategory.PresetColor presetColor : KeybindAtlasClientConfig.presetCategoryColors()) {
         if (KeybindAtlasClientConfig.findCategoryUsingFillColor(presetColor.fillColor()) == null) {
            return presetColor.fillColor();
         }
      }

      List<KeyCategory.PresetColor> presetColors = KeybindAtlasClientConfig.presetCategoryColors();
      return presetColors.isEmpty() ? KeyVisualStyle.keyAssignedFill() : presetColors.get(0).fillColor();
   }

   private void closeCategoryMode() {
      this.setHiddenKeysModeActive(false);
      if (this.categoryPanel != null) {
         this.categoryPanel.setExpanded(false);
      }
   }

   private void collapseOtherMenusForCategoryMode() {
      this.settingsOpen = false;
      this.layoutMenuOpen = false;
      this.restorePending = false;
      this.modFilterOpen = false;
      if (this.unassignedPanel != null) {
         this.unassignedPanel.setExpanded(false);
      }
   }

   private void toggleCategoriesEnabled() {
      boolean next = !this.categoriesEnabled();
      KeybindAtlasClientConfig.setCategoriesEnabled(next);
      this.selectCategory(this.selectedCategoryId);
      this.markStaticLayerDirty();
   }

   private void showStatusMessage(Component message) {
      this.statusMessage = message;
      this.statusMessageExpiry = Util.getMillis() + 3000L;
      this.statusMessageStartMs = Util.getMillis();
   }

   private boolean isBindingHidden(KeyboardKey key) {
      return this.bindingCache.hasBinding(key.glfwKey) && KeybindAtlasClientConfig.isHiddenBindingForKey(key.label, key.glfwKey);
   }

   private KeyCategory keyCategoryForKey(KeyboardKey key) {
      if (!this.bindingCache.hasBinding(key.glfwKey) && !this.bindingCache.hasBindingAnyLayer(key.glfwKey)) {
         return KeyCategory.assignedCategory();
      } else if (this.isBindingHidden(key)) {
         return KeyCategory.hiddenCategory();
      } else if (!this.categoriesEnabled()) {
         return KeyCategory.assignedCategory();
      } else {
         String customCatId = KeybindAtlasClientConfig.customCategoryIdForKey(key.label, key.glfwKey);
         if (customCatId != null && !customCatId.isEmpty()) {
            KeyCategory customCategory = KeybindAtlasClientConfig.findCustomCategory(customCatId);
            if (customCategory != null) {
               return customCategory;
            }
         }
         List<KeyAssignmentInfo> assigns = this.bindingCache.assignmentsForKey(key.glfwKey, this.currentLayer);
         if (!assigns.isEmpty()) {
            return KeybindAtlasClientConfig.findOrCreateCategory(assigns.get(0).category, assigns.get(0).categoryName);
         }
         List<KeyAssignmentInfo> all = this.bindingCache.allAssignmentsForKey(key.glfwKey);
         if (!all.isEmpty()) {
            return KeybindAtlasClientConfig.findOrCreateCategory(all.get(0).category, all.get(0).categoryName);
         }
         return KeyCategory.assignedCategory();
      }
   }

   private KeyboardRenderer.KeyRenderStyle keyRenderStyle(KeyboardKey key, boolean hovered) {
      if (this.isLayerModifierKey(key.glfwKey)) {
         return KeyboardRenderer.KeyRenderStyle.standard(
            hovered ? KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER : KeyboardRenderer.KeySpriteVariant.ASSIGNED
         );
      }
      int glfwKey = key.glfwKey;
      boolean hasBindingOnCurrentLayer = this.bindingCache.hasBinding(glfwKey, this.currentLayer);

      // 1. Direct Conflict on current layer: 2+ bindings mapped to the same key on this layer (RED FULL FILL)
      if (this.bindingCache.hasDirectConflict(glfwKey, this.currentLayer)) {
         int fill = KeyVisualStyle.keyDirectConflictFill();
         int text = KeyVisualStyle.keyDirectConflictText();
         return KeyboardRenderer.KeyRenderStyle.fullFill(fill, text, hovered);
      }

      // 2. Clean single binding on current layer -> Category Outline and Colored Label
      if (hasBindingOnCurrentLayer) {
         if (this.isBindingHidden(key)) {
            return KeyboardRenderer.KeyRenderStyle.standard(
               hovered ? KeyboardRenderer.KeySpriteVariant.HIDDEN_HOVER : KeyboardRenderer.KeySpriteVariant.HIDDEN
            );
         }
         KeyCategory category = this.keyCategoryForKey(key);
         if (category.isAssignedCategory()) {
            return KeyboardRenderer.KeyRenderStyle.standard(
               hovered ? KeyboardRenderer.KeySpriteVariant.ASSIGNED_HOVER : KeyboardRenderer.KeySpriteVariant.ASSIGNED
            );
         }
         return KeyboardRenderer.KeyRenderStyle.outline(
            category.fillColor(),
            KeyVisualStyle.keyAssignedFill(),
            category.fillColor(),
            hovered
         );
      }

      // 3. No bindings on current layer -> Unused
      return KeyboardRenderer.KeyRenderStyle.standard(
         hovered ? KeyboardRenderer.KeySpriteVariant.UNUSED_HOVER : KeyboardRenderer.KeySpriteVariant.UNUSED
      );
   }

   private String legendCategoryIdForKey(KeyboardKey key) {
      if (this.isLayerModifierKey(key.glfwKey)) {
         return "assigned";
      }
      int glfwKey = key.glfwKey;
      if (this.bindingCache.hasDirectConflict(glfwKey, this.currentLayer)) {
         return "__direct_conflict__";
      }
      if (this.bindingCache.hasBinding(glfwKey, this.currentLayer)) {
         return this.keyCategoryForKey(key).id();
      }
      return "__unused__";
   }

   private float legendAlphaForKey(KeyboardKey key) {
      return this.legendAlphaForCategoryId(this.legendCategoryIdForKey(key));
   }

   private float legendAlphaForCategoryId(String categoryId) {
      return this.legendFilterAlphaByCategory.getOrDefault(categoryId, this.targetLegendAlpha(categoryId));
   }

   private float targetLegendAlpha(String categoryId) {
      if (this.activeLegendHoverCategoryId != null && !this.activeLegendHoverCategoryId.isBlank()) {
         return this.activeLegendHoverCategoryId.equals(categoryId) ? 1.0F : 0.12F;
      } else {
         return 1.0F;
      }
   }

   private void updateLegendFilterAnimations(long now, List<KeyboardLegendOverlay.LegendEntry> legendEntries) {
      if (legendEntries.isEmpty()) {
         this.legendFilterAlphaByCategory.clear();
         this.lastLegendFilterAnimationNanos = -1L;
      } else {
         Set<String> visibleCategoryIds = new LinkedHashSet<>();

         for (KeyboardLegendOverlay.LegendEntry entry : legendEntries) {
            visibleCategoryIds.add(entry.categoryId());
         }

         this.legendFilterAlphaByCategory.keySet().removeIf(categoryIdx -> !visibleCategoryIds.contains(categoryIdx));
         if (!this.animationsEnabled()) {
            this.applyInstantLegendFilterState(visibleCategoryIds);
            this.lastLegendFilterAnimationNanos = now;
         } else {
            float deltaMs = this.lastLegendFilterAnimationNanos < 0L ? 140.0F : (float)(now - this.lastLegendFilterAnimationNanos) / 1000000.0F;
            this.lastLegendFilterAnimationNanos = now;
            float step = Math.min(1.0F, deltaMs / 140.0F);

            for (String categoryId : visibleCategoryIds) {
               float current = this.legendFilterAlphaByCategory.getOrDefault(categoryId, 1.0F);
               float target = this.targetLegendAlpha(categoryId);
               this.legendFilterAlphaByCategory.put(categoryId, this.moveTowards(current, target, step));
            }
         }
      }
   }

   private void applyInstantLegendFilterState(Set<String> visibleCategoryIds) {
      this.legendFilterAlphaByCategory.clear();

      for (String categoryId : visibleCategoryIds) {
         this.legendFilterAlphaByCategory.put(categoryId, this.targetLegendAlpha(categoryId));
      }
   }

   private int blendColor(int baseColor, int overlayColor, float amount) {
      float clamped = Math.max(0.0F, Math.min(1.0F, amount));
      int baseA = baseColor >>> 24 & 0xFF;
      int baseR = baseColor >>> 16 & 0xFF;
      int baseG = baseColor >>> 8 & 0xFF;
      int baseB = baseColor & 0xFF;
      int overlayA = overlayColor >>> 24 & 0xFF;
      int overlayR = overlayColor >>> 16 & 0xFF;
      int overlayG = overlayColor >>> 8 & 0xFF;
      int overlayB = overlayColor & 0xFF;
      int outA = Math.round((float)baseA + (float)(overlayA - baseA) * clamped);
      int outR = Math.round((float)baseR + (float)(overlayR - baseR) * clamped);
      int outG = Math.round((float)baseG + (float)(overlayG - baseG) * clamped);
      int outB = Math.round((float)baseB + (float)(overlayB - baseB) * clamped);
      return outA << 24 | outR << 16 | outG << 8 | outB;
   }

   private KeyboardRenderer.KeyRenderStyle fadedMouseStyle(KeyboardRenderer.KeyRenderStyle style, float alpha) {
      if (alpha >= 0.99F) {
         return style;
      }
      if (style.hasOutline()) {
         int fadedOutline = this.blendColor(KeyVisualStyle.keyboardFill(), style.outlineColor(), alpha);
         int fadedFill = this.blendColor(KeyVisualStyle.keyboardFill(), style.fillColor(), alpha);
         return this.renderer.keyStyle(fadedOutline, fadedFill, style.textColor(), false);
      }
      return this.renderer.keyStyle(this.blendColor(KeyVisualStyle.keyboardFill(), style.fillColor(), alpha), style.textColor(), false);
   }

   private List<KeyboardKey> legendSourceKeys() {
      return this.auxKeysActive && this.auxLayout != null ? this.auxLayout.keys : this.allKeys;
   }

   private List<KeyboardLegendOverlay.LegendEntry> buildLegendEntries() {
      Map<String, KeyCategory> categoriesInUse = new LinkedHashMap<>();
      int directConflictCount = 0;
      int unusedCount = 0;

      for (KeyboardKey key : this.legendSourceKeys()) {
         int glfwKey = key.glfwKey;
         if (this.bindingCache.hasDirectConflict(glfwKey, this.currentLayer)) {
            directConflictCount++;
         } else if (this.bindingCache.hasBinding(glfwKey, this.currentLayer)) {
            KeyCategory category = this.keyCategoryForKey(key);
            categoriesInUse.put(category.id(), category);
         } else {
            unusedCount++;
         }
      }

      List<KeyboardLegendOverlay.LegendEntry> entries = new ArrayList<>();

      if (directConflictCount > 0) {
         entries.add(
            KeyboardLegendOverlay.LegendEntry.custom(
               "__direct_conflict__",
               AtlasText.text("ui.legend.direct_conflict"),
               KeyVisualStyle.keyDirectConflictFill(),
               KeyVisualStyle.keyDirectConflictText()
            )
         );
      }

      for (KeyCategory category : categoriesInUse.values()) {
         if (category.isHiddenCategory()) {
            entries.add(KeyboardLegendOverlay.LegendEntry.standard(category.id(), category.name(), KeyboardRenderer.KeySpriteVariant.HIDDEN));
         } else if (category.isAssignedCategory()) {
            entries.add(KeyboardLegendOverlay.LegendEntry.standard(category.id(), category.name(), KeyboardRenderer.KeySpriteVariant.ASSIGNED));
         } else {
            entries.add(
               KeyboardLegendOverlay.LegendEntry.outline(
                  category.id(),
                  category.name(),
                  category.fillColor(),
                  KeyVisualStyle.keyAssignedFill(),
                  category.fillColor()
               )
            );
         }
      }

      if (unusedCount > 0) {
         entries.add(KeyboardLegendOverlay.LegendEntry.standard("__unused__", AtlasText.text("ui.legend.unused"), KeyboardRenderer.KeySpriteVariant.UNUSED));
      }

      return entries;
   }

   private void applySelectedCategoryToKey(KeyboardKey key) {
      if (key != null && this.bindingCache.hasBinding(key.glfwKey)) {
         KeyCategory previousCategory = this.keyCategoryForKey(key);
         KeyCategory category = this.selectedCategory();
         boolean clearSelectedCategory = previousCategory != null && previousCategory.id().equals(category.id()) && !category.isAssignedCategory();
         if (clearSelectedCategory) {
            KeybindAtlasClientConfig.setHiddenBindingForKey(key.label, key.glfwKey, false);
            KeybindAtlasClientConfig.clearCustomCategoryForKey(key.label, key.glfwKey);
         } else if (category.isHiddenCategory()) {
            if (!this.hiddenBindingsEnabled()) {
               KeybindAtlasClientConfig.setHiddenBindingsEnabled(true);
            }

            KeybindAtlasClientConfig.setHiddenBindingForKey(key.label, key.glfwKey, true);
            KeybindAtlasClientConfig.clearCustomCategoryForKey(key.label, key.glfwKey);
         } else if (category.isAssignedCategory()) {
            KeybindAtlasClientConfig.setHiddenBindingForKey(key.label, key.glfwKey, false);
            KeybindAtlasClientConfig.clearCustomCategoryForKey(key.label, key.glfwKey);
         } else {
            KeybindAtlasClientConfig.setHiddenBindingForKey(key.label, key.glfwKey, false);
            KeybindAtlasClientConfig.setCustomCategoryForKey(category.id(), key.label, key.glfwKey);
         }

         this.refreshAfterCategoryAssignmentChange(previousCategory, this.keyCategoryForKey(key));
      }
   }

   private void refreshAfterCategoryAssignmentChange(KeyCategory previousCategory, KeyCategory nextCategory) {
      boolean layoutChanged = previousCategory != null && previousCategory.isHiddenCategory() || nextCategory != null && nextCategory.isHiddenCategory();
      if (layoutChanged) {
         this.markBindingLayoutDirty();
      } else {
         this.markStaticLayerDirty();
      }
   }

   private void refreshAfterCategoryMetadataChange() {
      this.markStaticLayerDirty();
   }

   private void handleCategoryPanelAction(KeyCategoryPanel.PanelAction action) {
      switch (action.type) {
         case TOGGLE:
            if (this.categoryPanel != null) {
               if (!this.categoryPanel.isExpanded()) {
                  this.collapseOtherMenusForCategoryMode();
               }

               this.categoryPanel.toggle();
            }
            break;
         case SELECT_CATEGORY:
            this.selectCategory(action.categoryId);
            if (this.categoryPanel != null) {
               this.categoryPanel.setExpanded(false);
            }
            break;
         case CREATE_CATEGORY:
            if (this.categoryNameDialog != null) {
               this.categoryNameDialog.open(this.nextAvailableCategoryColor());
            }
            break;
         case EDIT_CATEGORY:
            if (this.categoryNameDialog != null) {
               KeyCategory categoryx = KeybindAtlasClientConfig.findCustomCategory(action.categoryId);
               if (categoryx != null) {
                  this.categoryNameDialog.openForEdit(categoryx);
               }
            }
            break;
         case DELETE_CATEGORY:
            KeyCategory category = KeybindAtlasClientConfig.findCustomCategory(action.categoryId);
            if (category != null && this.confirmDialog != null) {
               this.pendingDeleteCategoryId = category.id();
               this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.DELETE_CATEGORY;
               this.confirmDialog.open(AtlasText.text("ui.confirm.delete_category", category.name()));
            }
         case NONE:
      }
   }

   private void handleCategoryNameDialogResult(CategoryNameDialog.Result result) {
      if (result == CategoryNameDialog.Result.CANCEL) {
         if (this.categoryNameDialog != null) {
            this.categoryNameDialog.close();
         }
      } else {
         if (result == CategoryNameDialog.Result.CREATE && this.categoryNameDialog != null) {
            boolean editingCategory = this.categoryNameDialog.isEditingCategory();
            KeyCategory category = editingCategory
               ? KeybindAtlasClientConfig.updateCustomCategory(
                  this.categoryNameDialog.editingCategoryId(), this.categoryNameDialog.enteredName(), this.categoryNameDialog.selectedFillColor()
               )
               : KeybindAtlasClientConfig.createCustomCategory(this.categoryNameDialog.enteredName(), this.categoryNameDialog.selectedFillColor());
            if (category != null) {
               this.selectCategory(category.id());
               this.showStatusMessage(AtlasText.translatable(editingCategory ? "ui.status.category_updated" : "ui.status.category_created", category.name()));
               this.refreshAfterCategoryMetadataChange();
            }

            this.categoryNameDialog.close();
         }
      }
   }

   protected void init() {
      this.applyLayout(this.buildMainLayout());
      this.cachedLayoutFactory = new BoxTextLayoutFactory(this.font, this.pixelScale());
      this.cachedDebugOverlay = new KeyboardDebugOverlay(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.cachedLegendOverlay = new KeyboardLegendOverlay(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.cachedEditModeOverlay = new EditModeOverlay(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.categoryPanel = new KeyCategoryPanel(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.categoryNameDialog = new CategoryNameDialog(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.editDialog = new KeyBindingEditDialog(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.unassignedPanel = new UnassignedBindingsPanel(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.confirmDialog = new ConfirmationDialog(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
      this.layoutMenuOpen = false;
      this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.NONE;
      this.pendingDeleteCategoryId = "";
      this.selectedCategoryId = this.normalizeSelectedCategoryId(this.selectedCategoryId);
      this.auxLayout = null;
      this.auxStaticLayerPlan = KeyboardScreen.StaticLayerPlan.empty();
      this.auxStaticSettledBoxKeys = List.of();
      this.transitionActive = false;
      this.currentLayer = this.lockedLayer;
      this.bindingCache.setCurrentLayer(this.lockedLayer);
      this.markBindingLayoutDirty();
   }

   private void ensurePanelBlurResources() {
   }

   private int nativeMenuBackgroundBlurPasses() {
      Minecraft mc = this.minecraft != null ? this.minecraft : Minecraft.getInstance();
      int nativeBlur = mc == null ? 0 : mc.options.getMenuBackgroundBlurriness();
      return nativeBlur <= 0 ? 0 : Math.max(1, Math.min(5, (nativeBlur + 1) / 2));
   }

   private boolean shouldCapturePanelBlur() {
      if (this.nativeMenuBackgroundBlurPasses() <= 0) {
         return false;
      } else if (!this.isDebugMode) {
         return true;
      } else if ((this.confirmDialog == null || !this.confirmDialog.isVisible())
         && (this.categoryNameDialog == null || !this.categoryNameDialog.isVisible())
         && (this.editDialog == null || !this.editDialog.isVisible())
         && this.statusMessage == null
         && !this.settingsOpen
         && !this.modFilterOpen
         && !this.hiddenKeysModeActive) {
         return this.cachedEditModeOverlay != null && this.cachedEditModeOverlay.toolbarProgress() > 0.01F
            ? true
            : this.unassignedPanel != null && this.unassignedPanel.panelProgress() > 0.01F;
      } else {
         return true;
      }
   }

   private void disposePanelBlurResources() {
   }

   private void markBindingLayoutDirty() {
      this.bindingLayoutDirty = true;
      this.appliedBindingCacheSignature = Integer.MIN_VALUE;
      this.bindingCache.clearLayoutCache();
      cancelStalePreparedGeometryFutures();
      this.clearAuxBindingLayoutCache();
      this.clearDerivedLayoutCaches();
      this.hoveredBoxKey = null;
      this.debugIndex = -1;
      this.boxAnimStart.clear();
      this.hoverProgressByKey.clear();
      this.lastHoverAnimationNanos = -1L;
      this.bindingCacheRefreshInProgress = false;
      this.bindingCacheRefreshProcessedMappings = 0;
      this.bindingCacheRefreshTotalMappings = 0;
      this.geometryRefreshInProgress = false;
      this.geometryRefreshStartMs = 0L;
      this.rendererPrewarmInProgress = false;
      this.rendererPrewarmCompletedSteps = 0;
      this.rendererPrewarmTotalSteps = 0;
      this.activePreparedGeometryKey = null;
      this.rendererPrewarmState = null;
      this.prefetchFilterStateHash = Integer.MIN_VALUE;
      this.prefetchFilterIndex = 0;
      this.markStaticLayerDirty();
   }

   private void invalidateBindingCache() {
      this.bindingCache.markDirty();
      this.markBindingLayoutDirty();
   }

   private void markStaticLayerDirty() {
      this.staticLayerPlan = KeyboardScreen.StaticLayerPlan.empty();
      this.staticSettledBoxKeys = List.of();
      this.auxStaticLayerPlan = KeyboardScreen.StaticLayerPlan.empty();
      this.auxStaticSettledBoxKeys = List.of();
   }

   private static void cancelStalePreparedGeometryFutures() {
      int newGen = geometryGeneration.incrementAndGet();
      synchronized (KeyboardScreen.class) {
         if (!sharedPreparedGeometryFutures.isEmpty()) {
            LOGGER.warn("[GeometryDebug] cancelling {} stale futures, new gen={}", sharedPreparedGeometryFutures.size(), newGen);

            for (CompletableFuture<KeyboardScreen.PreparedGeometry> future : sharedPreparedGeometryFutures.values()) {
               future.cancel(false);
            }

            sharedPreparedGeometryFutures.clear();
         }
      }
   }

   private void clearAuxBindingLayoutCache() {
      this.cachedAuxBoxPositions = new LinkedHashMap<>();
      this.cachedAuxConnectorGeometry = List.of();
      this.auxBindingLayoutCached = false;
      this.auxVisibleBindingBoxCount = -1;
   }

   private void clearDerivedLayoutCaches() {
      this.filteredLayoutCache.clear();
      this.visibleAssignmentsByKey.clear();
      this.mainVisibleBindingBoxCount = -1;
      this.auxVisibleBindingBoxCount = -1;
      this.cachedVisibleAnimationKeys = null;
   }

   private void updateAssignmentFilterState() {
      this.assignmentFiltersActive = this.hasDisabledAssignments(this.modEnabled, this.bindingEnabled);
   }

   private boolean hasDisabledAssignments(Map<String, Boolean> mods, Map<String, Map<String, Boolean>> bindings) {
      for (Boolean enabled : mods.values()) {
         if (!Boolean.TRUE.equals(enabled)) {
            return true;
         }
      }

      for (Map<String, Boolean> modBindings : bindings.values()) {
         for (Boolean enabledx : modBindings.values()) {
            if (!Boolean.TRUE.equals(enabledx)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isAssignmentVisible(KeyAssignmentInfo assignment) {
      if (!this.assignmentFiltersActive) {
         return true;
      } else {
         Boolean modOn = this.modEnabled.get(assignment.source);
         if (modOn != null && !modOn) {
            return false;
         } else {
            Map<String, Boolean> modBindings = this.bindingEnabled.get(assignment.source);
            if (modBindings == null) {
               return true;
            } else {
               Boolean bindingOn = modBindings.get(assignment.label);
               return bindingOn == null || bindingOn;
            }
         }
      }
   }

   private boolean hasVisibleAssignments(int glfwKey) {
      if (!this.assignmentFiltersActive) {
         return this.bindingCache.hasBinding(glfwKey);
      } else {
         Boolean cached = this.visibleAssignmentsByKey.get(glfwKey);
         if (cached != null) {
            return cached;
         } else {
            boolean visible = false;

            for (KeyAssignmentInfo assignment : this.bindingCache.assignmentsForKey(glfwKey)) {
               if (this.isAssignmentVisible(assignment)) {
                  visible = true;
                  break;
               }
            }

            this.visibleAssignmentsByKey.put(glfwKey, visible);
            return visible;
         }
      }
   }

   private int countVisibleBindingBoxes(List<KeyboardKey> keys) {
      int count = 0;

      for (KeyboardKey key : keys) {
         if (this.bindingCache.hasBinding(key.glfwKey) && !this.isBindingHidden(key) && this.hasVisibleAssignments(key.glfwKey)) {
            count++;
         }
      }

      return count;
   }

   private boolean isCurrentAuxLayoutActive() {
      return this.auxLayout != null && this.allKeys == this.auxLayout.keys;
   }

   private int currentVisibleBindingBoxCount() {
      if (this.isCurrentAuxLayoutActive()) {
         if (this.auxVisibleBindingBoxCount < 0 && this.auxLayout != null) {
            this.auxVisibleBindingBoxCount = this.countVisibleBindingBoxes(this.auxLayout.keys);
         }

         return Math.max(0, this.auxVisibleBindingBoxCount);
      } else {
         if (this.mainVisibleBindingBoxCount < 0) {
            this.mainVisibleBindingBoxCount = this.countVisibleBindingBoxes(this.allKeys);
         }

         return Math.max(0, this.mainVisibleBindingBoxCount);
      }
   }

   private List<KeyboardKey> collectVisibleKeysWithBindings(List<KeyboardKey> keys) {
      List<KeyboardKey> result = new ArrayList<>();

      for (KeyboardKey key : keys) {
         if (this.bindingCache.hasBinding(key.glfwKey) && !this.isBindingHidden(key) && this.hasVisibleAssignments(key.glfwKey)) {
            result.add(key);
         }
      }

      return result;
   }

   private List<KeyboardKey> collectKeysWithAnyBindings(List<KeyboardKey> keys) {
      List<KeyboardKey> result = new ArrayList<>();

      for (KeyboardKey key : keys) {
         if (this.bindingCache.hasBinding(key.glfwKey)) {
            result.add(key);
         }
      }

      return result;
   }

   private int currentFilterStateHash() {
      return computeFilterStateHash(this.modSources, this.bindingsByMod, this.modEnabled, this.bindingEnabled);
   }

   private static int computeFilterStateHash(
      List<String> modSources, Map<String, List<String>> bindingsByMod, Map<String, Boolean> modEnabled, Map<String, Map<String, Boolean>> bindingEnabled
   ) {
      int hash = 1;

      for (String mod : modSources) {
         hash = 31 * hash + mod.hashCode();
         hash = 31 * hash + (Boolean.TRUE.equals(modEnabled.get(mod)) ? 1 : 0);

         for (String binding : bindingsByMod.getOrDefault(mod, List.of())) {
            hash = 31 * hash + binding.hashCode();
            Map<String, Boolean> modBindings = bindingEnabled.get(mod);
            boolean enabled = modBindings == null || !Boolean.FALSE.equals(modBindings.get(binding));
            hash = 31 * hash + (enabled ? 1 : 0);
         }
      }

      return hash;
   }

   private KeyboardScreen.PreparedGeometryKey buildPreparedGeometryKey(int completedSignature) {
      return new KeyboardScreen.PreparedGeometryKey(
         completedSignature,
         this.virtualWidth(),
         this.virtualHeight(),
         this.pixelScale(),
         this.currentFilterStateHash(),
         KeybindAtlasClientConfig.hiddenBindingLabelsHash(),
         this.fKeysEnabled(),
         this.hiddenBindingsEnabled(),
         this.keyboardLayoutPreset()
      );
   }

   private KeyboardScreen.PreparedGeometryRequest buildPreparedGeometryRequest(KeyboardScreen.PreparedGeometryKey key) {
      List<KeyboardKey> visibleKeys = new ArrayList<>();
      List<KeyboardKey> boundKeys = new ArrayList<>();

      for (KeyboardKey keyboardKey : this.allKeys) {
         if (this.bindingCache.hasBinding(keyboardKey.glfwKey)) {
            boundKeys.add(keyboardKey);
            if (!this.isBindingHidden(keyboardKey) && this.hasVisibleAssignments(keyboardKey.glfwKey)) {
               visibleKeys.add(keyboardKey);
            }
         }
      }

      List<KeyboardKey> keysWithBindings = List.copyOf(visibleKeys);
      List<KeyboardKey> keysWithAnyBindings = List.copyOf(boundKeys);
      int visibleBindingBoxCount = keysWithBindings.size();
      Map<Integer, BoxTextLayout> layoutByGlfw = new HashMap<>();

      for (KeyboardKey keyboardKeyx : keysWithBindings) {
         layoutByGlfw.putIfAbsent(keyboardKeyx.glfwKey, this.getFilteredBoxTextLayout(keyboardKeyx.glfwKey, false, visibleBindingBoxCount));
      }

      KeyboardLayout layout = new KeyboardLayout(
         this.allKeys,
         this.keyboardMinX,
         this.keyboardMaxX,
         this.keyboardMinY,
         this.keyboardMaxY,
         this.keyboardHousingX,
         this.keyboardHousingY,
         this.keyboardHousingWidth,
         this.keyboardHousingHeight
      );
      KeyboardScreen.PreparedGeometryReuseKey reuseKey = buildPreparedGeometryReuseKey(
         this.virtualWidth(), this.virtualHeight(), this.pixelScale(), layout, keysWithBindings, keysWithAnyBindings, layoutByGlfw
      );
      return new KeyboardScreen.PreparedGeometryRequest(
         key,
         this.virtualWidth(),
         this.virtualHeight(),
         this.pixelScale(),
         layout,
         keysWithBindings,
         keysWithAnyBindings,
         visibleBindingBoxCount,
         layoutByGlfw,
         this.cachedLayoutFactory.emptyLayout(),
         reuseKey
      );
   }

   private static KeyboardScreen.PreparedGeometryReuseKey buildPreparedGeometryReuseKey(
      int screenWidth,
      int screenHeight,
      int pixelScale,
      KeyboardLayout layout,
      List<KeyboardKey> keysWithBindings,
      List<KeyboardKey> keysWithAnyBindings,
      Map<Integer, BoxTextLayout> layoutByGlfw
   ) {
      int[] allKeyStates = new int[layout.keys.size() * 6];
      int allKeyIndex = 0;

      for (KeyboardKey key : layout.keys) {
         allKeyStates[allKeyIndex++] = key.glfwKey;
         allKeyStates[allKeyIndex++] = key.x;
         allKeyStates[allKeyIndex++] = key.y;
         allKeyStates[allKeyIndex++] = key.width;
         allKeyStates[allKeyIndex++] = key.height;
         allKeyStates[allKeyIndex++] = key.isMouseKey ? 1 : 0;
      }

      int[] visibleKeyStates = new int[keysWithBindings.size() * 3];
      int visibleKeyIndex = 0;

      for (KeyboardKey key : keysWithBindings) {
         BoxTextLayout boxLayout = layoutByGlfw.get(key.glfwKey);
         BoxDimensions dimensions = boxLayout == null ? new BoxDimensions(0, 0) : boxLayout.dimensions;
         visibleKeyStates[visibleKeyIndex++] = key.glfwKey;
         visibleKeyStates[visibleKeyIndex++] = dimensions.width;
         visibleKeyStates[visibleKeyIndex++] = dimensions.height;
      }

      int[] anyBindingKeys = new int[keysWithAnyBindings.size()];

      for (int i = 0; i < keysWithAnyBindings.size(); i++) {
         anyBindingKeys[i] = keysWithAnyBindings.get(i).glfwKey;
      }

      int[] mouseDeviceBounds = layout.mouseDeviceBounds == null ? new int[0] : Arrays.copyOf(layout.mouseDeviceBounds, layout.mouseDeviceBounds.length);
      return new KeyboardScreen.PreparedGeometryReuseKey(
         screenWidth,
         screenHeight,
         pixelScale,
         layout.minX,
         layout.maxX,
         layout.minY,
         layout.maxY,
         mouseDeviceBounds,
         allKeyStates,
         visibleKeyStates,
         anyBindingKeys
      );
   }

   private static KeyboardScreen.PreparedGeometry buildPreparedGeometry(KeyboardScreen.PreparedGeometryRequest request, int generation) {
      long startMs = System.currentTimeMillis();
      if (request.keysWithBindings.isEmpty()) {
         return KeyboardScreen.PreparedGeometry.empty(request.key, request.visibleBindingBoxCount);
      } else if (geometryGeneration.get() != generation) {
         LOGGER.warn("[GeometryDebug] build aborted pre-layout gen={} (current={})", generation, geometryGeneration.get());
         return null;
      } else {
         KeyboardScreen.SequentialGeometryResult geometry = buildSequentialGeometry(
            request.screenWidth,
            request.screenHeight,
            request.pixelScale,
            request.layout,
            request.keysWithBindings,
            request.keysWithAnyBindings,
            glfwKey -> request.layoutByGlfw.getOrDefault(glfwKey, request.emptyLayout),
            false,
            () -> geometryGeneration.get() != generation
         );
         long elapsedMs = System.currentTimeMillis() - startMs;
         if (geometryGeneration.get() != generation) {
            LOGGER.warn(
               "[GeometryDebug] build aborted post-route gen={} (current={}) elapsed={}ms", new Object[]{generation, geometryGeneration.get(), elapsedMs}
            );
            return null;
         } else {
            LOGGER.warn(
               "[GeometryDebug] build complete gen={} keys={} connectors={} elapsed={}ms",
               new Object[]{generation, request.keysWithBindings.size(), geometry.connectorGeometry.size(), elapsedMs}
            );
            return new KeyboardScreen.PreparedGeometry(request.key, geometry.boxPositions, geometry.connectorGeometry, request.visibleBindingBoxCount);
         }
      }
   }

   private static KeyboardScreen.SequentialGeometryResult buildSequentialGeometry(
      int screenWidth,
      int screenHeight,
      int pixelScale,
      KeyboardLayout layout,
      List<KeyboardKey> keysWithBindings,
      List<KeyboardKey> keysWithAnyBindings,
      Function<Integer, BoxTextLayout> layoutByKey,
      boolean debugMode,
      BooleanSupplier cancelled
   ) {
      if (keysWithBindings.isEmpty()) {
         return KeyboardScreen.SequentialGeometryResult.empty();
      } else {
         KeyboardBoxLayoutEngine layoutEngine = new KeyboardBoxLayoutEngine(screenWidth, screenHeight, pixelScale);
         int keyboardCenterX = (layout.minX + layout.maxX) / 2;
         Map<KeyboardKey, BoxPosition> templatePositions = layoutEngine.calculateBoxPositions(
            keysWithBindings, layout.keys, keyboardCenterX, layout.minX, layout.maxX, layout.minY, layout.maxY, layoutByKey, layout.mouseDeviceBounds
         );
         ConnectorRouteBuilder routeBuilder = new ConnectorRouteBuilder(
            layout.keys, keysWithAnyBindings, layout.minX, layout.maxX, layout.minY, layout.maxY, pixelScale, debugMode, screenWidth, screenHeight, cancelled
         );
         List<KeyboardKey> orderedKeys = routeBuilder.orderConnectorKeysCenterOut(keysWithBindings);
         Map<KeyboardKey, Integer> bindingIndexByKey = new HashMap<>();

         for (int index = 0; index < orderedKeys.size(); index++) {
            bindingIndexByKey.put(orderedKeys.get(index), index);
         }

         Map<KeyboardKey, BoxPosition> boxPositions = new LinkedHashMap<>();
         Map<BoxPosition, int[]> committedBoxRects = new LinkedHashMap<>();
         List<int[]> drawnSegments = new ArrayList<>();
         List<ConnectorGeometry> connectorGeometry = new ArrayList<>();

         for (KeyboardKey key : orderedKeys) {
            if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) {
               break;
            }

            BoxPosition templatePos = templatePositions.get(key);
            if (templatePos != null) {
               BoxPosition adjustedPos = layoutEngine.adjustSequentialBoxPosition(key, templatePos, committedBoxRects, drawnSegments);
               boxPositions.put(key, adjustedPos);
               ConnectorGeometry geometry = routeBuilder.buildForKey(
                  key, adjustedPos, bindingIndexByKey.getOrDefault(key, connectorGeometry.size()), drawnSegments, committedBoxRects
               );
               if (geometry == null) {
                  boxPositions.remove(key);
                  break;
               }

               connectorGeometry.add(geometry);
               routeBuilder.addRouteSegments(geometry.waypoints, drawnSegments);
            }
         }

         return new KeyboardScreen.SequentialGeometryResult(boxPositions, routeBuilder.finalizeGeometry(connectorGeometry));
      }
   }

   private static void cachePreparedGeometry(KeyboardScreen.PreparedGeometry preparedGeometry) {
      synchronized (KeyboardScreen.class) {
         sharedPreparedGeometryCache.put(preparedGeometry.key, preparedGeometry);
      }
   }

   private static void cachePreparedGeometryReuse(KeyboardScreen.PreparedGeometryReuseKey reuseKey, KeyboardScreen.PreparedGeometrySnapshot preparedGeometry) {
      synchronized (KeyboardScreen.class) {
         sharedPreparedGeometryReuseCache.put(reuseKey, preparedGeometry);
      }
   }

   private static void enqueuePreparedGeometryRequest(KeyboardScreen.PreparedGeometryKey key, KeyboardScreen.PreparedGeometryRequest request) {
      synchronized (KeyboardScreen.class) {
         if (!sharedPreparedGeometryCache.containsKey(key) && !sharedPreparedGeometryFutures.containsKey(key)) {
            KeyboardScreen.PreparedGeometrySnapshot reusableGeometry = sharedPreparedGeometryReuseCache.get(request.reuseKey);
            if (reusableGeometry != null) {
               sharedPreparedGeometryCache.put(key, reusableGeometry.toPreparedGeometry(key));
            } else {
               int generation = geometryGeneration.get();
               LOGGER.warn(
                  "[GeometryDebug] enqueue key filterHash={} gen={} visibleKeys={}",
                  new Object[]{key.filterStateHash, generation, request.keysWithBindings.size()}
               );
               CompletableFuture<KeyboardScreen.PreparedGeometry> future = CompletableFuture.<KeyboardScreen.PreparedGeometry>supplyAsync(
                     () -> buildPreparedGeometry(request, generation), GEOMETRY_EXECUTOR
                  )
                  .whenComplete((preparedGeometry, error) -> {
                     synchronized (KeyboardScreen.class) {
                        sharedPreparedGeometryFutures.remove(key);
                        if (error != null) {
                           LOGGER.error("[GeometryDebug] build FAILED gen={}", generation, error);
                        } else if (preparedGeometry == null) {
                           LOGGER.warn("[GeometryDebug] build STALE gen={} (current={})", generation, geometryGeneration.get());
                        } else {
                           cachePreparedGeometry(preparedGeometry);
                           cachePreparedGeometryReuse(request.reuseKey, preparedGeometry.snapshot());
                           LOGGER.warn("[GeometryDebug] build OK gen={} boxes={}", generation, preparedGeometry.boxPositions.size());
                        }
                     }
                  });
               sharedPreparedGeometryFutures.put(key, future);
            }
         }
      }
   }

   private KeyboardScreen.PreparedGeometry getSharedPreparedGeometry(KeyboardScreen.PreparedGeometryKey key) {
      synchronized (KeyboardScreen.class) {
         return sharedPreparedGeometryCache.get(key);
      }
   }

   private void ensurePreparedGeometryRefreshStarted(KeyboardScreen.PreparedGeometryKey key) {
      synchronized (KeyboardScreen.class) {
         if (sharedPreparedGeometryCache.containsKey(key) || sharedPreparedGeometryFutures.containsKey(key)) {
            return;
         }
      }

      KeyboardScreen.PreparedGeometryRequest request = this.buildPreparedGeometryRequest(key);
      enqueuePreparedGeometryRequest(key, request);
   }

   private void applyPreparedGeometry(KeyboardScreen.PreparedGeometry preparedGeometry) {
      this.cachedBoxPositions = new LinkedHashMap<>(preparedGeometry.boxPositions);
      this.cachedConnectorGeometry = List.copyOf(preparedGeometry.connectorGeometry);
      this.mainVisibleBindingBoxCount = preparedGeometry.visibleBindingBoxCount;
      this.hoveredBoxKey = null;
      Set<Integer> visibleAnimationKeys = this.buildVisibleAnimationKeys();
      this.boxAnimStart.keySet().retainAll(visibleAnimationKeys);
      this.markStaticLayerDirty();
      this.bindingLayoutDirty = false;
      this.appliedBindingCacheSignature = preparedGeometry.key.bindingSignature;
      this.geometryRefreshInProgress = false;
      this.geometryRefreshStartMs = 0L;
      this.activePreparedGeometryKey = preparedGeometry.key;
      this.rendererPrewarmInProgress = false;
      this.rendererPrewarmCompletedSteps = 0;
      this.rendererPrewarmTotalSteps = 0;
      this.rendererPrewarmState = null;
   }

   private boolean ensureRendererPrewarmed() {
      if (this.renderer == null || this.activePreparedGeometryKey == null) {
         this.rendererPrewarmInProgress = false;
         this.rendererPrewarmCompletedSteps = 0;
         this.rendererPrewarmTotalSteps = 0;
         this.rendererPrewarmState = null;
         return false;
      } else if (sharedRendererWarmKeys.contains(this.activePreparedGeometryKey)) {
         this.rendererPrewarmInProgress = false;
         this.rendererPrewarmCompletedSteps = 0;
         this.rendererPrewarmTotalSteps = 0;
         this.rendererPrewarmState = null;
         return true;
      } else {
         if (this.rendererPrewarmState == null || !this.activePreparedGeometryKey.equals(this.rendererPrewarmState.key)) {
            this.rendererPrewarmState = this.buildRendererPrewarmState(this.activePreparedGeometryKey);
            this.rendererPrewarmTotalSteps = this.rendererPrewarmState.totalSteps();
         }

         this.rendererPrewarmState.prewarmNext(this.renderer, Math.max(1, 12));
         this.rendererPrewarmCompletedSteps = this.rendererPrewarmState.completedSteps();
         this.rendererPrewarmTotalSteps = this.rendererPrewarmState.totalSteps();
         this.rendererPrewarmInProgress = !this.rendererPrewarmState.isComplete();
         if (!this.rendererPrewarmInProgress) {
            synchronized (KeyboardScreen.class) {
               sharedRendererWarmKeys.add(this.activePreparedGeometryKey);

               while (sharedRendererWarmKeys.size() > 24) {
                  KeyboardScreen.PreparedGeometryKey oldestKey = sharedRendererWarmKeys.iterator().next();
                  sharedRendererWarmKeys.remove(oldestKey);
               }
            }

            this.rendererPrewarmState = null;
         }

         return !this.rendererPrewarmInProgress;
      }
   }

   private KeyboardScreen.RendererPrewarmState buildRendererPrewarmState(KeyboardScreen.PreparedGeometryKey key) {
      Set<KeyboardScreen.RendererKeySpriteTask> keyTasks = new HashSet<>();
      Set<KeyboardScreen.RendererPanelSpriteTask> panelTasks = new HashSet<>();
      Set<KeyboardScreen.RendererLabelTask> labelTasks = new HashSet<>();
      Map<Integer, KeyboardRenderer.KeyRenderStyle> keyStyles = new HashMap<>();
      List<KeyboardKey> mouseKeys = new ArrayList<>();

      for (KeyboardKey keyboardKey : this.allKeys) {
         boolean hasBinding = this.bindingCache.hasBinding(keyboardKey.glfwKey);
         KeyboardRenderer.KeyRenderStyle style = this.keyRenderStyle(keyboardKey, false);
         keyStyles.put(keyboardKey.glfwKey, style);
         if (keyboardKey.isMouseKey) {
            mouseKeys.add(keyboardKey);
         } else {
            keyTasks.add(new KeyboardScreen.RendererKeySpriteTask(keyboardKey.width, keyboardKey.height, style));
            if (hasBinding) {
               labelTasks.add(new KeyboardScreen.RendererLabelTask(keyboardKey.label, keyboardKey.width, keyboardKey.height));
            }
         }
      }

      for (ConnectorGeometry geometry : this.cachedConnectorGeometry) {
         BoxTextLayout layout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, false);
         if (!layout.rows.isEmpty()) {
            panelTasks.add(new KeyboardScreen.RendererPanelSpriteTask(layout.dimensions.width, layout.dimensions.height, false));
         }
      }

      KeyboardLayout mainLayout = this.buildMainLayout();
      int[] mouseBounds = mainLayout.mouseDeviceBounds;
      return new KeyboardScreen.RendererPrewarmState(
         key,
         this.keyboardHousingWidth,
         this.keyboardHousingHeight,
         mouseBounds,
         mouseKeys,
         keyStyles,
         List.copyOf(keyTasks),
         List.copyOf(panelTasks),
         List.copyOf(labelTasks)
      );
   }

   private void ensureBindingLayoutCache() {
      if (this.minecraft != null && this.minecraft.options != null && this.font != null) {
         BoxTextLayoutFactory layoutFactory = this.cachedLayoutFactory;
         if (layoutFactory != null) {
            KeyboardBindingCache.RefreshResult refreshResult = this.bindingCache.refreshIfNeeded(this.minecraft.options.keyMappings, 24);
            this.bindingCacheRefreshInProgress = !refreshResult.complete;
            this.bindingCacheRefreshProcessedMappings = refreshResult.processedMappings;
            this.bindingCacheRefreshTotalMappings = refreshResult.totalMappings;
            if (!refreshResult.complete) {
               this.bindingLayoutDirty = true;
               this.geometryRefreshInProgress = false;
               this.geometryRefreshStartMs = 0L;
               this.activePreparedGeometryKey = null;
               this.rendererPrewarmState = null;
               this.clearAuxBindingLayoutCache();
               this.hoveredBoxKey = null;
               this.debugIndex = -1;
               this.markStaticLayerDirty();
            } else {
               int completedSignature = this.bindingCache.completedSignature();
               if (this.bindingLayoutDirty || this.appliedBindingCacheSignature != completedSignature || this.geometryRefreshInProgress) {
                  List<String> sources = this.bindingCache.sourcesWithAssignments();
                  Map<String, Boolean> newMap = new LinkedHashMap<>();

                  for (String s : sources) {
                     Boolean prev = this.modEnabled.get(s);
                     newMap.put(s, prev == null ? Boolean.TRUE : prev);
                  }

                  List<String> persistedDisabled = KeybindAtlasClientConfig.disabledMods();
                  if (persistedDisabled != null && !persistedDisabled.isEmpty()) {
                     for (String ds : persistedDisabled) {
                        if (ds != null && newMap.containsKey(ds)) {
                           newMap.put(ds, Boolean.FALSE);
                        }
                     }
                  }

                  this.modSources = sources;
                  this.modEnabled = newMap;
                  Map<String, List<String>> newBindingsByMod = this.bindingCache.bindingsBySource();
                  Map<String, Map<String, Boolean>> newBindingEnabled = new LinkedHashMap<>();

                  for (String s : sources) {
                     Map<String, Boolean> prevModBindings = this.bindingEnabled.getOrDefault(s, Map.of());
                     Map<String, Boolean> newModBindings = new LinkedHashMap<>();

                     for (String b : newBindingsByMod.getOrDefault(s, List.of())) {
                        Boolean prev = prevModBindings.get(b);
                        newModBindings.put(b, prev == null ? Boolean.TRUE : prev);
                     }

                     newBindingEnabled.put(s, newModBindings);
                  }

                  List<String> persistedDisabledBindings = KeybindAtlasClientConfig.disabledBindings();
                  if (persistedDisabledBindings != null) {
                     for (String entry : persistedDisabledBindings) {
                        int sep = entry.indexOf("::");
                        if (sep > 0) {
                           String src = entry.substring(0, sep);
                           String lbl = entry.substring(sep + 2);
                           Map<String, Boolean> modBindings = newBindingEnabled.get(src);
                           if (modBindings != null && modBindings.containsKey(lbl)) {
                              modBindings.put(lbl, Boolean.FALSE);
                           }
                        }
                     }
                  }

                  this.bindingsByMod = newBindingsByMod;
                  this.bindingEnabled = newBindingEnabled;
                  this.updateAssignmentFilterState();
                  this.clearDerivedLayoutCaches();
                  this.clearAuxBindingLayoutCache();
                  int totalDisplayRows = KeyboardLegendOverlay.displayRowCount(this.modSources, this.modExpanded, this.bindingsByMod, this.modExpandProgress);
                  int maxStart = Math.max(0, totalDisplayRows - 8);
                  if (this.modPanelScroll > maxStart) {
                     this.modPanelScroll = maxStart;
                  }

                  KeyboardScreen.PreparedGeometryKey geometryKey = this.buildPreparedGeometryKey(completedSignature);
                  KeyboardScreen.PreparedGeometry preparedGeometry = this.getSharedPreparedGeometry(geometryKey);
                  if (preparedGeometry == null) {
                     this.ensurePreparedGeometryRefreshStarted(geometryKey);
                     preparedGeometry = this.getSharedPreparedGeometry(geometryKey);
                  }

                  if (preparedGeometry == null) {
                     if (!this.geometryRefreshInProgress) {
                        this.geometryRefreshStartMs = Util.getMillis();
                        LOGGER.warn(
                           "[GeometryDebug] waiting for geometry filterHash={} futuresPending={} cacheSize={}",
                           new Object[]{geometryKey.filterStateHash, sharedPreparedGeometryFutures.size(), sharedPreparedGeometryCache.size()}
                        );
                     }

                     this.geometryRefreshInProgress = true;
                     this.activePreparedGeometryKey = null;
                     this.rendererPrewarmState = null;
                     this.clearAuxBindingLayoutCache();
                     this.hoveredBoxKey = null;
                     this.debugIndex = -1;
                     this.markStaticLayerDirty();
                  } else {
                     this.applyPreparedGeometry(preparedGeometry);
                     this.debugIndex = Math.min(this.debugIndex, this.cachedConnectorGeometry.size() - 1);
                     if (this.modFilterOpen) {
                        this.prefetchFilterGeometries(completedSignature);
                     }
                  }
               }
            }
         }
      }
   }

   private void prefetchFilterGeometries(int completedSignature) {
      if (this.modSources != null && !this.modSources.isEmpty()) {
         synchronized (KeyboardScreen.class) {
            if (!sharedPreparedGeometryFutures.isEmpty()) {
               return;
            }
         }

         int currentHash = this.currentFilterStateHash();
         if (currentHash != this.prefetchFilterStateHash) {
            this.prefetchFilterStateHash = currentHash;
            this.prefetchFilterIndex = 0;
         }

         int pixScale = this.pixelScale();
         boolean fKeys = this.fKeysEnabled();
         boolean hiddenBindings = this.hiddenBindingsEnabled();
         BoxTextLayoutFactory layoutFactory = this.cachedLayoutFactory;
         if (layoutFactory != null) {
            int modsCount = this.modSources.size();
            int attempt = 0;

            Map<String, Boolean> hypotheticalModEnabled;
            KeyboardScreen.PreparedGeometryKey hypotheticalKey;
            while (true) {
               if (attempt >= modsCount) {
                  return;
               }

               int idx = this.prefetchFilterIndex % modsCount;
               this.prefetchFilterIndex++;
               String mod = this.modSources.get(idx);
               Boolean currentState = this.modEnabled.get(mod);
               if (currentState != null) {
                  hypotheticalModEnabled = new LinkedHashMap<>(this.modEnabled);
                  hypotheticalModEnabled.put(mod, !currentState);
                  int hypotheticalFilterHash = computeFilterStateHash(this.modSources, this.bindingsByMod, hypotheticalModEnabled, this.bindingEnabled);
                  hypotheticalKey = new KeyboardScreen.PreparedGeometryKey(
                     completedSignature,
                     this.virtualWidth(),
                     this.virtualHeight(),
                     pixScale,
                     hypotheticalFilterHash,
                     KeybindAtlasClientConfig.hiddenBindingLabelsHash(),
                     fKeys,
                     hiddenBindings,
                     this.keyboardLayoutPreset()
                  );
                  synchronized (KeyboardScreen.class) {
                     if (!sharedPreparedGeometryCache.containsKey(hypotheticalKey) && !sharedPreparedGeometryFutures.containsKey(hypotheticalKey)) {
                        break;
                     }
                  }
               }

               attempt++;
            }

            KeyboardScreen.PreparedGeometryRequest request = this.buildPrefetchGeometryRequest(hypotheticalKey, hypotheticalModEnabled, layoutFactory);
            if (request != null) {
               enqueuePreparedGeometryRequest(hypotheticalKey, request);
            }
         }
      }
   }

   private KeyboardScreen.PreparedGeometryRequest buildPrefetchGeometryRequest(
      KeyboardScreen.PreparedGeometryKey key, Map<String, Boolean> hypotheticalModEnabled, BoxTextLayoutFactory layoutFactory
   ) {
      boolean filtersActive = this.hasDisabledAssignments(hypotheticalModEnabled, this.bindingEnabled);
      List<KeyboardKey> visibleKeys = new ArrayList<>();
      List<KeyboardKey> boundKeys = new ArrayList<>();

      for (KeyboardKey keyboardKey : this.allKeys) {
         if (this.bindingCache.hasBinding(keyboardKey.glfwKey)) {
            boundKeys.add(keyboardKey);
            if (!this.isBindingHidden(keyboardKey) && this.hasVisibleAssignmentsForState(keyboardKey.glfwKey, filtersActive, hypotheticalModEnabled)) {
               visibleKeys.add(keyboardKey);
            }
         }
      }

      List<KeyboardKey> keysWithBindings = List.copyOf(visibleKeys);
      List<KeyboardKey> keysWithAnyBindings = List.copyOf(boundKeys);
      int visibleBindingBoxCount = keysWithBindings.size();
      Map<Integer, BoxTextLayout> layoutByGlfw = new HashMap<>();

      for (KeyboardKey keyboardKeyx : keysWithBindings) {
         layoutByGlfw.putIfAbsent(
            keyboardKeyx.glfwKey,
            this.buildFilteredLayout(keyboardKeyx.glfwKey, false, visibleBindingBoxCount, filtersActive, hypotheticalModEnabled, layoutFactory)
         );
      }

      KeyboardLayout layout = new KeyboardLayout(
         this.allKeys,
         this.keyboardMinX,
         this.keyboardMaxX,
         this.keyboardMinY,
         this.keyboardMaxY,
         this.keyboardHousingX,
         this.keyboardHousingY,
         this.keyboardHousingWidth,
         this.keyboardHousingHeight
      );
      KeyboardScreen.PreparedGeometryReuseKey reuseKey = buildPreparedGeometryReuseKey(
         this.virtualWidth(), this.virtualHeight(), this.pixelScale(), layout, keysWithBindings, keysWithAnyBindings, layoutByGlfw
      );
      return new KeyboardScreen.PreparedGeometryRequest(
         key,
         this.virtualWidth(),
         this.virtualHeight(),
         this.pixelScale(),
         layout,
         keysWithBindings,
         keysWithAnyBindings,
         visibleBindingBoxCount,
         layoutByGlfw,
         layoutFactory.emptyLayout(),
         reuseKey
      );
   }

   private boolean hasVisibleAssignmentsForState(int glfwKey, boolean filtersActive, Map<String, Boolean> hypotheticalModEnabled) {
      if (!filtersActive) {
         return this.bindingCache.hasBinding(glfwKey);
      } else {
         for (KeyAssignmentInfo assignment : this.bindingCache.assignmentsForKey(glfwKey)) {
            if (this.isAssignmentVisibleForState(assignment, hypotheticalModEnabled)) {
               return true;
            }
         }

         return false;
      }
   }

   private boolean isAssignmentVisibleForState(KeyAssignmentInfo assignment, Map<String, Boolean> hypotheticalModEnabled) {
      Boolean modOn = hypotheticalModEnabled.get(assignment.source);
      if (modOn != null && !modOn) {
         return false;
      } else {
         Map<String, Boolean> modBindings = this.bindingEnabled.get(assignment.source);
         if (modBindings == null) {
            return true;
         } else {
            Boolean bindingOn = modBindings.get(assignment.label);
            return bindingOn == null || bindingOn;
         }
      }
   }

   private BoxTextLayout buildFilteredLayout(
      int glfwKey,
      boolean hovered,
      int visibleBindingBoxCount,
      boolean filtersActive,
      Map<String, Boolean> hypotheticalModEnabled,
      BoxTextLayoutFactory layoutFactory
   ) {
      List<KeyAssignmentInfo> assigns = this.bindingCache.assignmentsForKey(glfwKey);
      if (assigns != null && !assigns.isEmpty() && filtersActive) {
         List<KeyAssignmentInfo> filtered = new ArrayList<>();

         for (KeyAssignmentInfo assignment : assigns) {
            if (this.isAssignmentVisibleForState(assignment, hypotheticalModEnabled)) {
               filtered.add(assignment);
            }
         }

         return layoutFactory.create(filtered, hovered, false, visibleBindingBoxCount);
      } else {
         return this.bindingCache.getBoxTextLayout(glfwKey, hovered, false, visibleBindingBoxCount, layoutFactory);
      }
   }

   public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
      boolean _profiling = KeybindAtlasClientConfig.renderProfilingEnabled();
      long _pTotal = _profiling ? System.nanoTime() : 0L;
      long _pBlur = _profiling ? System.nanoTime() : 0L;
      this.renderScreenBackground(guiGraphics, partialTick);
      if (_profiling) {
         this.profileAccumBlur = this.profileAccumBlur + (System.nanoTime() - _pBlur);
      }

      float s = this.responsiveScale();
      int vMouseX = Math.round((float) mouseX / s);
      int vMouseY = Math.round((float) mouseY / s);

      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().scale(s, s);
      try {
         this.updateActiveLayer();
         this.ensureBindingLayoutCache();
         this.ensureRenderer();
         if (this.renderer != null) {
            boolean loadingActive = this.bindingCacheRefreshInProgress || this.geometryRefreshInProgress;
            if (!loadingActive && !this.ensureRendererPrewarmed()) {
               loadingActive = true;
            }

            List<KeyboardLegendOverlay.LegendEntry> legendEntries = this.isDebugMode ? List.of() : this.buildLegendEntries();
            int pixelScale = this.pixelScale();
            int screenCenterX = this.virtualWidth() / 2;
            int screenCenterY = this.virtualHeight() / 2;
            long now = System.nanoTime();
            this.activeLegendHoverCategoryId = "";
            if (!loadingActive
               && !this.isDebugMode
               && !this.hiddenKeysModeActive
               && !this.editModeState.isActive()
               && this.cachedLegendOverlay != null
               && this.font != null) {
               this.activeLegendHoverCategoryId = this.cachedLegendOverlay.hoveredLegendCategoryId(vMouseX, vMouseY, this.font, legendEntries);
            }

            this.updateLegendFilterAnimations(now, legendEntries);
            String auxToggleTooltipId = null;
            Component auxToggleTooltip = null;
            if (this.transitionActive) {
               float transElapsed = (float)(now - this.transitionStartNanos) / 1000000.0F;
               if (transElapsed >= 500.0F) {
                  this.completeTransition(now);
               }
            }

            OverlayRenderHelper.pushScaleTransform(guiGraphics, screenCenterX, screenCenterY, pixelScale);
            if (this.transitionActive) {
               this.renderTransitionKeyboards(guiGraphics, this.renderer, pixelScale, now);
               this.captureBlurBackground(guiGraphics, partialTick);
            } else if (loadingActive) {
               this.updatePanelAnimations(now);
               this.ensureStaticLayerPlan(now, false);

               try {
                  KeyboardScreen.StaticLayerPlan activePlan = this.staticLayerPlan;
                  this.renderStaticLayerBase(guiGraphics, this.renderer, activePlan);
                  this.renderConnectorLines(guiGraphics, this.renderer, now);
                  this.captureBlurBackground(guiGraphics, partialTick);
                  this.renderStaticLayerBoxes(guiGraphics, this.renderer, activePlan);
               } catch (Exception var54) {
               }
            } else {
               if (this.isPointOverAnyOpenPanel(vMouseX, vMouseY)) {
                  this.hoveredKeyX = -1;
                  this.hoveredKeyY = -1;
               } else {
                  this.hoveredKeyX = OverlayRenderHelper.scaleX(vMouseX, screenCenterX, pixelScale);
                  this.hoveredKeyY = OverlayRenderHelper.scaleY(vMouseY, screenCenterY, pixelScale);
               }

            if (!this.auxKeysActive) {
               this.frameVisibleGeometry = this.computeFrameVisibleGeometry(this.cachedConnectorGeometry);
               this.updateHoverAnimations(now);
               this.updateHoveredBoxKey(this.renderer, now);
            }

            this.updatePanelAnimations(now);
            this.prepareVisibleAnimations(now);
            this.ensureStaticLayerPlan(now, false);
            List<KeyboardKey> prevButtons = null;
            Map<KeyboardKey, BoxPosition> prevBoxPos = null;
            List<ConnectorGeometry> prevConn = null;
            int[] prevBounds = null;
            if (this.auxKeysActive) {
               this.ensureAuxBindingLayoutCache();
               if (this.auxLayout != null) {
                  prevButtons = this.allKeys;
                  prevBoxPos = this.cachedBoxPositions;
                  prevConn = this.cachedConnectorGeometry;
                  prevBounds = this.saveState();
                  this.applyLayout(this.auxLayout);
                  this.cachedBoxPositions = this.cachedAuxBoxPositions;
                  this.cachedConnectorGeometry = this.cachedAuxConnectorGeometry;
                  if (this.auxActivatedNanos > 0L) {
                     int visibleIndex = 0;

                     for (ConnectorGeometry geometry : this.cachedAuxConnectorGeometry) {
                        int animKey = this.animationKey(geometry.button);
                        if (!this.boxAnimStart.containsKey(animKey)) {
                           this.boxAnimStart.put(animKey, now + (long)visibleIndex * 50000000L);
                           visibleIndex++;
                        }
                     }
                  }

                  this.prepareVisibleAnimations(now);
                  this.frameVisibleGeometry = this.computeFrameVisibleGeometry(this.cachedConnectorGeometry);
                  this.cachedVisibleAnimationKeys = null;
                  this.updateHoverAnimations(now);
                  this.updateHoveredBoxKey(this.renderer, now);
                  this.ensureStaticLayerPlan(now, true);
               }
            }

            try {
               KeyboardScreen.StaticLayerPlan activePlan = this.auxKeysActive ? this.auxStaticLayerPlan : this.staticLayerPlan;
               long _pBase = _profiling ? System.nanoTime() : 0L;
               this.renderStaticLayerBase(guiGraphics, this.renderer, activePlan);
               this.renderHoveredKeys(guiGraphics, this.renderer);
               if (_profiling) {
                  this.profileAccumBase = this.profileAccumBase + (System.nanoTime() - _pBase);
               }

               long _pConn = _profiling ? System.nanoTime() : 0L;
               this.renderConnectorLines(guiGraphics, this.renderer, now);
               if (_profiling) {
                  this.profileAccumConnectors = this.profileAccumConnectors + (System.nanoTime() - _pConn);
               }

               long _pCBlur = _profiling ? System.nanoTime() : 0L;
               this.captureBlurBackground(guiGraphics, partialTick);
               if (_profiling) {
                  this.profileAccumCaptureBlur = this.profileAccumCaptureBlur + (System.nanoTime() - _pCBlur);
               }

               long _pBoxes = _profiling ? System.nanoTime() : 0L;
               this.renderStaticLayerBoxes(guiGraphics, this.renderer, activePlan);
               if (_profiling) {
                  this.profileAccumBoxes = this.profileAccumBoxes + (System.nanoTime() - _pBoxes);
               }

               long _pDyn = _profiling ? System.nanoTime() : 0L;
               this.renderDynamicBoxes(guiGraphics, this.renderer, now);
               if (_profiling) {
                  this.profileAccumDynamic = this.profileAccumDynamic + (System.nanoTime() - _pDyn);
               }

               this.renderDebugGeometry(guiGraphics);
            } finally {
               if (prevButtons != null) {
                  this.restoreState(prevButtons, prevBoxPos, prevConn, prevBounds);
               }
            }
         }

         if (KeybindAtlasClientConfig.allKeysEnabled() && this.renderer != null) {
            int[] btn = this.auxToggleButtonBounds(pixelScale);
            boolean showingExtraKeys = this.transitionActive ? this.transitionToAux : this.auxKeysActive;
            String label = showingExtraKeys ? "<" : ">";
            int smx = OverlayRenderHelper.scaleX(vMouseX, screenCenterX, pixelScale);
            int smy = OverlayRenderHelper.scaleY(vMouseY, screenCenterY, pixelScale);
            boolean auxHovered = !this.isPointOverAnyOpenPanel(vMouseX, vMouseY) && OverlayRenderHelper.isMouseOver(smx, smy, btn[0], btn[1], btn[2], btn[3], 0);
            this.renderer.drawHudButton(guiGraphics, btn[0], btn[1], btn[2], btn[3], label, auxHovered, false);
            if (auxHovered) {
               auxToggleTooltipId = showingExtraKeys ? "aux-toggle-main" : "aux-toggle-extra";
               auxToggleTooltip = AtlasText.translatable(showingExtraKeys ? "ui.tooltip.button.show_main_keys" : "ui.tooltip.button.show_extra_keys");
            }
         }

         guiGraphics.pose().popMatrix();
         int activeDebugIndex = this.debugIndex;
         int activeBindingCount = this.auxKeysActive ? this.cachedAuxConnectorGeometry.size() : this.cachedConnectorGeometry.size();
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
         long _pOverlays = _profiling ? System.nanoTime() : 0L;
         float categoryModeHudAlpha = this.easeInOut(this.categoryModeHudProgress);
         KeyboardDebugOverlay overlay = this.cachedDebugOverlay;

         try {
            if (this.editDialog != null && this.editDialog.isVisible()) {
               this.hoveredBoxKey = null;
            }

            if (this.categoryNameDialog != null && this.categoryNameDialog.isVisible()) {
               this.hoveredBoxKey = null;
            }

            if (this.confirmDialog != null && this.confirmDialog.isVisible()) {
               this.hoveredBoxKey = null;
            }

            if (this.cachedEditModeOverlay != null
               && this.cachedEditModeOverlay.isPointOverToolbar(vMouseX, vMouseY, this.editModeState.isActive(), this.hiddenKeysModeActive)) {
               this.hoveredBoxKey = null;
            }

            long _pDbg = _profiling ? System.nanoTime() : 0L;
            overlay.render(
               guiGraphics,
               this.renderer,
               this.font,
               vMouseX,
               vMouseY,
               this.isDebugMode,
               this.scaleIndex,
               this.keyboardLayoutPreset(),
               this.animationsEnabled(),
               this.categoriesEnabled(),
               this.hiddenKeysModeActive,
               this.categoryInfoVisible,
               categoryModeHudAlpha,
               this.fKeysEnabled(),
               activeDebugIndex,
               activeBindingCount,
               this.settingsOpen,
               this.restorePending,
               this.settingsDebugExpanded,
               this.layoutMenuOpen,
               this.easeInOut(this.settingsPanelProgress)
            );
            if (_profiling) {
               this.profileAccumDebugOverlay = this.profileAccumDebugOverlay + (System.nanoTime() - _pDbg);
            }
         } catch (Exception var53) {
            this.showStatusMessage(AtlasText.translatable("ui.status.ui_render_error", var53.getMessage()));
         }

         long _pLeg = _profiling ? System.nanoTime() : 0L;
         if (!this.isDebugMode) {
            KeyboardLegendOverlay legend = this.cachedLegendOverlay;
            boolean effectiveModFilterOpen = this.modFilterOpen && !loadingActive;
            float effectiveModFilterProgress = loadingActive ? 0.0F : this.easeInOut(this.modFilterPanelProgress);
            legend.render(
               guiGraphics,
               this.renderer,
               this.font,
               legendEntries,
               this.modSources,
               this.modEnabled,
               this.modExpanded,
               this.bindingsByMod,
               this.bindingEnabled,
               this.modExpandProgress,
               effectiveModFilterOpen,
               vMouseX,
               vMouseY,
               this.modPanelScroll,
               effectiveModFilterProgress,
               !loadingActive
            );
         }

         if (_profiling) {
            this.profileAccumLegend = this.profileAccumLegend + (System.nanoTime() - _pLeg);
         }

         if (this.deferredHoveredEntry != null && this.renderer != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
            OverlayRenderHelper.pushScaleTransform(guiGraphics, screenCenterX, screenCenterY, pixelScale);
            this.renderer
               .drawDynamicBox(
                  guiGraphics,
                  this.deferredHoveredEntry.x,
                  this.deferredHoveredEntry.y,
                  this.deferredHoveredEntry.layout,
                  this.deferredHoveredEntry.hovered,
                  this.deferredHoveredEntry.boxAlpha,
                  this.deferredHoveredEntry.textAlpha
               );
            if (this.editModeState.isActive() && (this.editDialog == null || !this.editDialog.isVisible())) {
               guiGraphics.pose().pushMatrix();
               guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
               this.drawEditButtons(guiGraphics, this.renderer, this.deferredHoveredEntry.x, this.deferredHoveredEntry.y, this.deferredHoveredEntry.layout);
               guiGraphics.pose().popMatrix();
            }

            guiGraphics.pose().popMatrix();
            guiGraphics.pose().popMatrix();
            this.deferredHoveredEntry = null;
         }

         long _pEdit = _profiling ? System.nanoTime() : 0L;
         if (!this.isDebugMode && !loadingActive && this.cachedEditModeOverlay != null && this.renderer != null) {
            this.cachedEditModeOverlay
               .render(
                  guiGraphics,
                  this.renderer,
                  this.font,
                  vMouseX,
                  vMouseY,
                  this.editModeState.isActive(),
                  this.editModeState.canUndo(),
                  this.hiddenKeysModeActive,
                  this.categoriesEnabled()
               );
         }

         if (!loadingActive
            && this.categoryPanel != null
            && this.renderer != null
            && (this.hiddenKeysModeActive || categoryModeHudAlpha > 0.01F)
            && !this.isDebugMode
            && !this.editModeState.isActive()) {
            this.categoryPanel
               .render(
                  guiGraphics,
                  this.renderer,
                  this.font,
                  vMouseX,
                  vMouseY,
                  this.categoryPanelEntries(),
                  this.selectedCategoryId,
                  KeybindAtlasClientConfig.presetCategoryColors(),
                  categoryModeHudAlpha
               );
         }

         float editToolbarAlpha = this.cachedEditModeOverlay != null ? this.easeInOut(this.cachedEditModeOverlay.toolbarProgress()) : 0.0F;
         if (!loadingActive && editToolbarAlpha > 0.01F && this.unassignedPanel != null && this.renderer != null && !this.isDebugMode) {
            int totalUnassigned = this.editModeState.countUnassigned(this.minecraft.options.keyMappings);
            this.unassignedPanel.render(guiGraphics, this.renderer, this.font, vMouseX, vMouseY, this.unassignedByMod, totalUnassigned, editToolbarAlpha);
         }

         if (this.editDialog != null && this.editDialog.isVisible() && this.renderer != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
            this.editDialog.render(guiGraphics, this.renderer, this.font, vMouseX, vMouseY);
            guiGraphics.pose().popMatrix();
         }

         if (this.categoryNameDialog != null && this.categoryNameDialog.isVisible() && this.renderer != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
            this.categoryNameDialog
               .render(
                  guiGraphics, this.renderer, this.font, vMouseX, vMouseY, KeybindAtlasClientConfig.presetCategoryColors(), this.categoryDialogUsedColorOwners()
               );
            guiGraphics.pose().popMatrix();
         }

         if (this.confirmDialog != null && this.confirmDialog.isVisible() && this.renderer != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
            this.confirmDialog.render(guiGraphics, this.renderer, this.font, vMouseX, vMouseY);
            guiGraphics.pose().popMatrix();
         }

         if (_profiling) {
            this.profileAccumEditOverlay = this.profileAccumEditOverlay + (System.nanoTime() - _pEdit);
         }

         this.auxToggleTooltipState.update(auxToggleTooltipId, vMouseX, vMouseY);
         if (auxToggleTooltipId != null && auxToggleTooltip != null && this.font != null) {
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
            OverlayRenderHelper.pushScaleTransform(guiGraphics, screenCenterX, screenCenterY, pixelScale);
            this.auxToggleTooltipState
               .renderIfReady(guiGraphics, this.font, this.virtualWidth(), this.virtualHeight(), pixelScale, vMouseX, vMouseY, auxToggleTooltipId, List.of(auxToggleTooltip));
            guiGraphics.pose().popMatrix();
            guiGraphics.pose().popMatrix();
         }

         guiGraphics.pose().popMatrix();
         long _pStatus = _profiling ? System.nanoTime() : 0L;
         this.renderLayersPanel(guiGraphics, vMouseX, vMouseY);
         this.renderBindingRefreshIndicator(guiGraphics);
         this.renderHiddenKeysInfoPanel(guiGraphics);
         if (this.statusMessage != null) {
            long nowMs = Util.getMillis();
            float elapsed = (float)(nowMs - this.statusMessageStartMs);
            float totalDuration = (float)(this.statusMessageExpiry - this.statusMessageStartMs);
            float fadeIn = Math.min(1.0F, elapsed / 200.0F);
            fadeIn = 1.0F - (1.0F - fadeIn) * (1.0F - fadeIn);
            float fadeOutStart = totalDuration - 300.0F;
            float fadeOut = 1.0F;
            if (elapsed > fadeOutStart) {
               fadeOut = Math.max(0.0F, 1.0F - (elapsed - fadeOutStart) / 300.0F);
               fadeOut *= fadeOut;
            }

            float fadeAlpha = fadeIn * fadeOut;
            if (elapsed > totalDuration) {
               this.statusMessage = null;
            } else if (fadeAlpha > 0.02F) {
               int msgW = this.font.width(this.statusMessage);
               int msgX = (this.virtualWidth() - msgW) / 2;
               int msgY = this.virtualHeight() - 48;
               int padX = 6;
               int padY = 4;
               int boxX = msgX - padX;
               int boxY = msgY - padY;
               int boxW = msgW + padX * 2;
               int boxH = 9 + padY * 2;
               int cr = 1;
               guiGraphics.pose().pushMatrix();
               guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
               OverlayRenderHelper.blitBlurredBackground(guiGraphics, boxX, boxY, boxW, boxH, boxX, boxY, boxW, boxH, fadeAlpha);
               OverlayRenderHelper.fillRoundedRect(
                  guiGraphics,
                  boxX,
                  boxY,
                  boxW,
                  boxH,
                  cr,
                  OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), KeyVisualStyle.panelAlpha() * fadeAlpha)
               );
               OverlayRenderHelper.drawRoundedRectOutline(
                  guiGraphics, boxX, boxY, boxW, boxH, cr, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), fadeAlpha), 1
               );
               guiGraphics.text(this.font, this.statusMessage, msgX, msgY, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxText(), fadeAlpha), true);
               guiGraphics.pose().popMatrix();
            }
         }

         OverlayRenderHelper.setPanelBlurTarget(null, 0, 0);
         if (_profiling) {
            this.profileAccumStatusMsg = this.profileAccumStatusMsg + (System.nanoTime() - _pStatus);
         }

         if (_profiling) {
            this.profileAccumOverlays = this.profileAccumOverlays + (System.nanoTime() - _pOverlays);
            this.profileAccumTotal = this.profileAccumTotal + (System.nanoTime() - _pTotal);
            this.profileFrameCount++;
            if (this.profileFrameCount >= 120) {
               double div = (double)this.profileFrameCount * 1000000.0;
               LOGGER.info(
                  "[RenderProfile] frames={} scale={} | blur={} base={} conn={} capBlur={} boxes={} dyn={} overlays={} (debug={} legend={} edit={} super={} status={}) TOTAL={}",
                  new Object[]{
                     this.profileFrameCount,
                     this.pixelScale(),
                     String.format("%.2fms", (double)this.profileAccumBlur / div),
                     String.format("%.2fms", (double)this.profileAccumBase / div),
                     String.format("%.2fms", (double)this.profileAccumConnectors / div),
                     String.format("%.2fms", (double)this.profileAccumCaptureBlur / div),
                     String.format("%.2fms", (double)this.profileAccumBoxes / div),
                     String.format("%.2fms", (double)this.profileAccumDynamic / div),
                     String.format("%.2fms", (double)this.profileAccumOverlays / div),
                     String.format("%.2fms", (double)this.profileAccumDebugOverlay / div),
                     String.format("%.2fms", (double)this.profileAccumLegend / div),
                     String.format("%.2fms", (double)this.profileAccumEditOverlay / div),
                     String.format("%.2fms", (double)this.profileAccumSuperRender / div),
                     String.format("%.2fms", (double)this.profileAccumStatusMsg / div),
                     String.format("%.2fms", (double)this.profileAccumTotal / div)
                  }
               );
               this.profileFrameCount = 0;
               this.profileAccumBlur = 0L;
               this.profileAccumBase = 0L;
               this.profileAccumConnectors = 0L;
               this.profileAccumCaptureBlur = 0L;
               this.profileAccumBoxes = 0L;
               this.profileAccumDynamic = 0L;
               this.profileAccumOverlays = 0L;
               this.profileAccumTotal = 0L;
               this.profileAccumDebugOverlay = 0L;
               this.profileAccumLegend = 0L;
               this.profileAccumEditOverlay = 0L;
               this.profileAccumSuperRender = 0L;
               this.profileAccumStatusMsg = 0L;
            }
         }
      }
   } finally {
      guiGraphics.pose().popMatrix();
   }
}

   private void renderBindingRefreshIndicator(GuiGraphicsExtractor guiGraphics) {
      if (this.font != null) {
         Component message = this.loadingProgressMessage();
         if (message != null) {
            int msgW = this.font.width(message);
            int msgX = (this.virtualWidth() - msgW) / 2;
            int msgY = this.virtualHeight() - 68;
            int padX = 6;
            int padY = 4;
            int boxX = msgX - padX;
            int boxY = msgY - padY;
            int boxW = msgW + padX * 2;
            int boxH = 9 + padY * 2;
            int cr = 1;
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
            OverlayRenderHelper.blitBlurredBackground(guiGraphics, boxX, boxY, boxW, boxH, boxX, boxY, boxW, boxH);
            OverlayRenderHelper.fillRoundedRect(
               guiGraphics,
               boxX,
               boxY,
               boxW,
               boxH,
               cr,
               OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), KeyVisualStyle.panelAlpha())
            );
            OverlayRenderHelper.drawRoundedRectOutline(guiGraphics, boxX, boxY, boxW, boxH, cr, KeyVisualStyle.boxBorder(), 1);
            guiGraphics.text(this.font, message, msgX, msgY, KeyVisualStyle.boxText(), true);
            guiGraphics.pose().popMatrix();
         }
      }
   }

   private Component loadingProgressMessage() {
      if (this.bindingCacheRefreshInProgress && this.bindingCacheRefreshTotalMappings > 0) {
         int percent = Math.min(
            79, Math.round((float)this.bindingCacheRefreshProcessedMappings * 79.0F / (float)Math.max(1, this.bindingCacheRefreshTotalMappings))
         );
         return AtlasText.translatable("ui.status.loading_bindings", percent);
      } else if (this.geometryRefreshInProgress) {
         long elapsedMs = Math.max(0L, Util.getMillis() - this.geometryRefreshStartMs);
         int percent = Math.min(94, 80 + (int)(elapsedMs / 90L));
         return AtlasText.translatable("ui.status.preparing_layout", percent);
      } else if (this.rendererPrewarmInProgress && this.rendererPrewarmTotalSteps > 0) {
         int percent = 95 + Math.min(4, Math.round((float)this.rendererPrewarmCompletedSteps * 4.0F / (float)Math.max(1, this.rendererPrewarmTotalSteps)));
         return AtlasText.translatable("ui.status.preparing_graphics", percent);
      } else {
         return null;
      }
   }

   private void captureBlurBackground(GuiGraphicsExtractor guiGraphics, float partialTick) {
   }

   private void renderScreenBackground(GuiGraphicsExtractor guiGraphics, float partialTick) {
      if (this.isDebugMode) {
         guiGraphics.fill(0, 0, this.width, this.height, OverlayRenderHelper.opaque(OverlayRenderHelper.darken(KeyVisualStyle.keyboardFill(), 0.12F)));
      } else {
         guiGraphics.fill(0, 0, this.width, this.height, -1073741824);
      }
   }

   private void renderTransitionKeyboards(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, int pixelScale, long now) {
      float transElapsed = (float)(now - this.transitionStartNanos) / 1000000.0F;
      boolean boxFadePhase = transElapsed < 150.0F;
      KeyboardLayout mainLayout = new KeyboardLayout(
         this.allKeys,
         this.keyboardMinX,
         this.keyboardMaxX,
         this.keyboardMinY,
         this.keyboardMaxY,
         this.keyboardHousingX,
         this.keyboardHousingY,
         this.keyboardHousingWidth,
         this.keyboardHousingHeight
      );
      KeyboardLayout outgoing = this.transitionToAux ? mainLayout : this.auxLayout;
      KeyboardLayout incoming = this.transitionToAux ? this.auxLayout : mainLayout;
      if (boxFadePhase) {
         float fadeProgress = this.easeOut(transElapsed / 150.0F);
         float boxAlpha = 1.0F - fadeProgress;
         if (this.transitionToAux) {
            this.renderKeyboardBase(guiGraphics, renderer, outgoing);
            this.renderConnectorLinesFaded(guiGraphics, renderer, now, boxAlpha);
            this.renderBoxesFaded(guiGraphics, renderer, now, boxAlpha);
         } else {
            List<KeyboardKey> prevButtons = this.allKeys;
            Map<KeyboardKey, BoxPosition> prevBoxPos = this.cachedBoxPositions;
            List<ConnectorGeometry> prevConn = this.cachedConnectorGeometry;
            int[] prevBounds = this.saveState();

            try {
               this.ensureAuxBindingLayoutCache();
               this.applyLayout(this.auxLayout);
               this.cachedBoxPositions = this.cachedAuxBoxPositions;
               this.cachedConnectorGeometry = this.cachedAuxConnectorGeometry;
               this.renderKeyboardBase(guiGraphics, renderer, outgoing);
               this.renderConnectorLinesFaded(guiGraphics, renderer, now, boxAlpha);
               this.renderBoxesFaded(guiGraphics, renderer, now, boxAlpha);
            } finally {
               this.restoreState(prevButtons, prevBoxPos, prevConn, prevBounds);
            }
         }
      } else {
         float slideProgress = this.easeInOut(Math.min(1.0F, (transElapsed - 150.0F) / 350.0F));
         int fullWidth = this.virtualWidth() * pixelScale;
         int slideAmount = Math.round((float)fullWidth * slideProgress);
         int dir = this.transitionToAux ? 1 : -1;
         int outgoingOffset = -dir * slideAmount;
         int incomingOffset = dir * (fullWidth - slideAmount);
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)((float)outgoingOffset), (float)(0.0F));
         this.renderKeyboardBase(guiGraphics, renderer, outgoing);
         guiGraphics.pose().popMatrix();
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)((float)incomingOffset), (float)(0.0F));
         this.renderKeyboardBase(guiGraphics, renderer, incoming);
         guiGraphics.pose().popMatrix();
      }
   }

   private void renderKeyboardBase(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, KeyboardLayout layout) {
      if (layout != null) {
         if (layout.housingWidth > 0 && layout.housingHeight > 0) {
            renderer.drawKeyboardHousing(guiGraphics, layout.housingX, layout.housingY, layout.housingWidth, layout.housingHeight);
         }

         if (layout.mouseDeviceBounds != null) {
            List<KeyboardKey> mouseKeys = new ArrayList<>();

            for (KeyboardKey k : layout.keys) {
               if (k.isMouseKey) {
                  mouseKeys.add(k);
               }
            }

            int[] mb = layout.mouseDeviceBounds;
            renderer.drawMouseDevice(
               guiGraphics, mb[0], mb[1], mb[2], mb[3], mouseKeys, kx -> this.fadedMouseStyle(this.keyRenderStyle(kx, false), this.legendAlphaForKey(kx))
            );
         }

         for (KeyboardKey button : layout.keys) {
            if (!button.isMouseKey) {
               renderer.drawKey(guiGraphics, button, this.keyRenderStyle(button, false), true, this.legendAlphaForKey(button));
            }
         }
      }
   }

   private void renderStaticLayerBase(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, KeyboardScreen.StaticLayerPlan plan) {
      if (plan != null && !plan.isEmpty()) {
         renderer.drawKeyboardHousing(guiGraphics, plan.housingX, plan.housingY, plan.housingWidth, plan.housingHeight);
         List<KeyboardKey> mouseKeys = new ArrayList<>();
         Map<Integer, KeyboardRenderer.KeyRenderStyle> mouseStyles = new HashMap<>();

         for (KeyboardScreen.StaticKeyEntry key : plan.keys) {
            float alpha = this.legendAlphaForKey(key.button);
            if (key.button.isMouseKey) {
               mouseKeys.add(key.button);
               mouseStyles.put(key.button.glfwKey, this.fadedMouseStyle(key.style, alpha));
            } else {
               renderer.drawKey(guiGraphics, key.button, key.style, true, alpha);
            }
         }

         if (plan.mouseDeviceBounds != null && !mouseKeys.isEmpty()) {
            int[] bounds = plan.mouseDeviceBounds;
            renderer.drawMouseDevice(
               guiGraphics,
               bounds[0],
               bounds[1],
               bounds[2],
               bounds[3],
               mouseKeys,
               keyx -> mouseStyles.getOrDefault(keyx.glfwKey, this.keyRenderStyle(keyx, false))
            );
         }
      }
   }

   private void renderStaticLayerBoxes(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, KeyboardScreen.StaticLayerPlan plan) {
      if (plan != null && !plan.boxes.isEmpty()) {
         for (KeyboardScreen.StaticBoxEntry box : plan.boxes) {
            float alpha = this.legendAlphaForCategoryId(box.legendCategoryId);
            renderer.drawDynamicBox(guiGraphics, box.x, box.y, box.layout, false, alpha, alpha);
         }
      }
   }

   private void renderConnectorLinesFaded(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, long now, float alpha) {
      if (!(alpha <= 0.01F)) {
         boolean alternatingLineColors = KeybindAtlasClientConfig.alternatingLineColorsEnabled();
         renderer.beginPolyLineBatch(guiGraphics);

         for (ConnectorGeometry geometry : this.cachedConnectorGeometry) {
            if (this.isBindingVisible(geometry) && !this.getFilteredBoxTextLayout(geometry.button.glfwKey, false).rows.isEmpty()) {
               float elapsedMs = this.getBoxElapsedMs(geometry.button, now);
               float lineProgress = this.easeOut(Math.min(1.0F, Math.max(0.0F, (elapsedMs - 350.0F) / 220.0F)));
               float legendAlpha = this.legendAlphaForKey(geometry.button);
               int baseColor = alternatingLineColors && geometry.useDarkColor ? KeyVisualStyle.lineDefaultAlt() : KeyVisualStyle.lineDefault();
               int fadedColor = OverlayRenderHelper.withAlpha(baseColor, alpha * legendAlpha);
               renderer.drawPolyLineProgress(guiGraphics, geometry.waypoints, fadedColor, lineProgress, geometry.totalLineLength);
            }
         }

         renderer.endPolyLineBatch();
      }
   }

   private void renderBoxesFaded(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, long now, float alpha) {
      if (!(alpha <= 0.01F)) {
         for (ConnectorGeometry geometry : this.cachedConnectorGeometry) {
            if (this.isBindingVisible(geometry)) {
               BoxTextLayout layout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, false);
               if (!layout.rows.isEmpty()) {
                  KeyboardScreen.StaticBoxEntry placement = this.resolveBoxPlacement(geometry, layout);
                  float legendAlpha = this.legendAlphaForCategoryId(placement.legendCategoryId);
                  renderer.drawDynamicBox(guiGraphics, placement.x, placement.y, layout, false, alpha * legendAlpha, alpha * legendAlpha);
               }
            }
         }
      }
   }

   private void completeTransition(long now) {
      this.transitionActive = false;
      this.auxKeysActive = this.transitionToAux;
      this.debugIndex = -1;
      this.hoverProgressByKey.clear();
      this.hoveredBoxKey = null;
      this.boxAnimStart.clear();
      this.cachedVisibleAnimationKeys = null;
      if (this.auxKeysActive) {
         this.auxActivatedNanos = now;
         this.ensureAuxBindingLayoutCache();
         if (this.auxLayout != null) {
            int idx = 0;

            for (ConnectorGeometry geometry : this.cachedAuxConnectorGeometry) {
               this.boxAnimStart.put(this.animationKey(geometry.button), now + (long)idx * 50000000L);
               idx++;
            }
         }
      } else {
         this.auxActivatedNanos = -1L;
         int idx = 0;

         for (ConnectorGeometry geometry : this.cachedConnectorGeometry) {
            this.boxAnimStart.put(this.animationKey(geometry.button), now + (long)idx * 50000000L);
            idx++;
         }
      }

      this.markStaticLayerDirty();
      this.lastHoverAnimationNanos = -1L;
   }

   private void updateHoveredBoxKey(KeyboardRenderer renderer, long now) {
      int previousHoveredGlfw = this.hoveredBoxKey != null ? this.hoveredBoxKey.glfwKey : -1;
      this.hoveredBoxKey = null;
      List<ConnectorGeometry> visible = this.frameVisibleGeometry;
      if (previousHoveredGlfw >= 0) {
         ConnectorGeometry previousGeometry = this.findVisibleGeometryByGlfw(visible, previousHoveredGlfw);
         boolean previousKeyHovered = previousGeometry != null && previousGeometry.button.isHovered(this.hoveredKeyX, this.hoveredKeyY, this.pixelScale());
         if (previousGeometry != null && (previousKeyHovered || this.isMouseOverGeometryBox(renderer, previousGeometry, now, previousKeyHovered))) {
            this.hoveredBoxKey = previousGeometry.button;
            return;
         }
      }

      for (int i = visible.size() - 1; i >= 0; i--) {
         ConnectorGeometry geometry = visible.get(i);
         if (geometry.button.isHovered(this.hoveredKeyX, this.hoveredKeyY, this.pixelScale())) {
            this.hoveredBoxKey = geometry.button;
            return;
         }
      }

      for (int ix = visible.size() - 1; ix >= 0; ix--) {
         ConnectorGeometry geometry = visible.get(ix);
         boolean keyHovered = geometry.button.isHovered(this.hoveredKeyX, this.hoveredKeyY, this.pixelScale());
         if (this.isMouseOverGeometryBox(renderer, geometry, now, keyHovered)) {
            this.hoveredBoxKey = geometry.button;
            return;
         }
      }
   }

   private ConnectorGeometry findVisibleGeometryByGlfw(List<ConnectorGeometry> visible, int glfwKey) {
      for (ConnectorGeometry geometry : visible) {
         if (geometry.button.glfwKey == glfwKey) {
            return geometry;
         }
      }

      return null;
   }

   private boolean isMouseOverGeometryBox(KeyboardRenderer renderer, ConnectorGeometry geometry, long now, boolean hovered) {
      BoxPosition dynamicBounds = this.currentDynamicBoxBounds(geometry, now, hovered);
      if (dynamicBounds != null && renderer.isMouseInBox(this.hoveredKeyX, this.hoveredKeyY, dynamicBounds)) {
         return true;
      } else {
         BoxTextLayout compactLayout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, false);
         KeyboardScreen.StaticBoxEntry compactPlacement = this.resolveBoxPlacement(geometry, compactLayout);
         if (this.isMouseOverPlacement(renderer, compactPlacement)) {
            return true;
         } else {
            BoxTextLayout expandedLayout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, true);
            float currentHoverProgress = this.hoverProgress(geometry.button);
            boolean preserveExpandedHover = hovered || currentHoverProgress > 0.01F;
            if (preserveExpandedHover) {
               KeyboardScreen.StaticBoxEntry expandedPlacement = this.resolveBoxPlacement(geometry, expandedLayout);
               float visualProgress = hovered ? Math.max(currentHoverProgress, 0.01F) : this.hoverVisualProgress(geometry.button, currentHoverProgress, false);
               BoxTextLayout transitionLayout = visualProgress >= 0.99F
                  ? expandedLayout
                  : (visualProgress <= 0.01F ? compactLayout : this.interpolateHoverLayout(compactLayout, expandedLayout, visualProgress));
               if (transitionLayout != compactLayout && this.isMouseOverPlacement(renderer, this.resolveBoxPlacement(geometry, transitionLayout))) {
                  return true;
               }

               if (this.isMouseOverPlacement(renderer, expandedPlacement)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private boolean isMouseOverPlacement(KeyboardRenderer renderer, KeyboardScreen.StaticBoxEntry placement) {
      return renderer.isMouseInBox(
         this.hoveredKeyX,
         this.hoveredKeyY,
         new BoxPosition(placement.x, placement.y, 0, 0, placement.layout.dimensions.width, placement.layout.dimensions.height)
      );
   }

   private BoxPosition currentDynamicBoxBounds(ConnectorGeometry geometry, long now, boolean hovered) {
      BoxTextLayout compactLayout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, false);
      float currentHoverProgress = this.hoverProgress(geometry.button);
      float elapsedMs = this.getBoxElapsedMs(geometry.button, now);
      boolean settled = elapsedMs >= 570.0F;
      if (settled && currentHoverProgress <= 0.01F) {
         return null;
      } else {
         float boxAlpha = this.easeOut(Math.min(1.0F, elapsedMs / 350.0F));
         int slidePixels = 8 * this.pixelScale();
         int slideX = geometry.boxLeft
            ? Math.round((1.0F - boxAlpha) * (float)slidePixels)
            : (geometry.boxRight ? -Math.round((1.0F - boxAlpha) * (float)slidePixels) : 0);
         int slideY = geometry.boxAbove
            ? Math.round((1.0F - boxAlpha) * (float)slidePixels)
            : (geometry.boxBelow ? -Math.round((1.0F - boxAlpha) * (float)slidePixels) : 0);
         BoxTextLayout expandedLayout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, true);
         float visualProgress = this.hoverVisualProgress(geometry.button, currentHoverProgress, hovered);
         BoxTextLayout layout = visualProgress <= 0.01F
            ? compactLayout
            : (visualProgress >= 0.99F ? expandedLayout : this.interpolateHoverLayout(compactLayout, expandedLayout, visualProgress));
         KeyboardScreen.StaticBoxEntry placement = this.resolveBoxPlacement(geometry, layout);
         return new BoxPosition(placement.x + slideX, placement.y + slideY, 0, 0, layout.dimensions.width, layout.dimensions.height);
      }
   }

   private void ensureRenderer() {
      if (this.minecraft != null && this.font != null) {
         if (sharedRenderer == null || !sharedRenderer.matches(this.minecraft, this.font, this.pixelScale())) {
            this.disposeRenderer();
            sharedRenderer = new KeyboardRenderer(this.minecraft, this.font, this.pixelScale());
            this.markStaticLayerDirty();
         }

         this.renderer = sharedRenderer;
      } else {
         this.renderer = null;
      }
   }

   private void disposeRenderer() {
      if (sharedRenderer != null) {
         sharedRenderer.close();
         sharedRenderer = null;
      }

      synchronized (KeyboardScreen.class) {
         sharedRendererWarmKeys.clear();
      }

      this.renderer = null;
   }

   private void detachRenderer() {
      this.renderer = null;
   }

   private void ensureAuxLayout() {
      if (this.auxLayout == null || this.auxLayout.keys.isEmpty()) {
         this.auxLayout = KeyboardLayouts.buildAuxiliaryKeysLayout(this.virtualWidth(), this.virtualHeight(), this.pixelScale());
         this.clearAuxBindingLayoutCache();
         this.auxStaticLayerPlan = KeyboardScreen.StaticLayerPlan.empty();
         this.auxStaticSettledBoxKeys = List.of();
      }
   }

   private List<KeyboardKey> collectKeysWithBindings(List<KeyboardKey> keys) {
      return this.collectVisibleKeysWithBindings(keys);
   }

   private void ensureAuxBindingLayoutCache() {
      this.ensureAuxLayout();
      if (!this.bindingCacheRefreshInProgress && !this.geometryRefreshInProgress && !this.auxBindingLayoutCached && this.auxLayout != null) {
         List<KeyboardKey> prevButtons = this.allKeys;
         Map<KeyboardKey, BoxPosition> prevBoxPos = this.cachedBoxPositions;
         List<ConnectorGeometry> prevConn = this.cachedConnectorGeometry;
         int[] prevBounds = this.saveState();

         try {
            this.applyLayout(this.auxLayout);
            this.auxVisibleBindingBoxCount = this.countVisibleBindingBoxes(this.auxLayout.keys);
            List<KeyboardKey> auxKeysWithBindings = this.collectKeysWithBindings(this.auxLayout.keys);
            if (auxKeysWithBindings.isEmpty()) {
               this.cachedBoxPositions = new LinkedHashMap<>();
               this.cachedConnectorGeometry = List.of();
            } else {
               this.buildLayoutGeometry(auxKeysWithBindings);
            }

            this.cachedAuxBoxPositions = this.cachedBoxPositions;
            this.cachedAuxConnectorGeometry = this.cachedConnectorGeometry;
            this.auxBindingLayoutCached = true;
         } finally {
            this.restoreState(prevButtons, prevBoxPos, prevConn, prevBounds);
         }
      }
   }

   private int[] saveState() {
      return new int[]{
         this.keyboardMinX,
         this.keyboardMaxX,
         this.keyboardMinY,
         this.keyboardMaxY,
         this.keyboardHousingX,
         this.keyboardHousingY,
         this.keyboardHousingWidth,
         this.keyboardHousingHeight
      };
   }

   private void applyLayout(KeyboardLayout layout) {
      this.allKeys = layout.keys;
      this.keyboardMinX = layout.minX;
      this.keyboardMaxX = layout.maxX;
      this.keyboardMinY = layout.minY;
      this.keyboardMaxY = layout.maxY;
      this.keyboardHousingX = layout.housingX;
      this.keyboardHousingY = layout.housingY;
      this.keyboardHousingWidth = layout.housingWidth;
      this.keyboardHousingHeight = layout.housingHeight;
   }

   private void restoreState(List<KeyboardKey> prevButtons, Map<KeyboardKey, BoxPosition> prevBoxPos, List<ConnectorGeometry> prevConn, int[] prevBounds) {
      this.allKeys = prevButtons;
      this.cachedBoxPositions = prevBoxPos;
      this.cachedConnectorGeometry = prevConn;
      this.keyboardMinX = prevBounds[0];
      this.keyboardMaxX = prevBounds[1];
      this.keyboardMinY = prevBounds[2];
      this.keyboardMaxY = prevBounds[3];
      this.keyboardHousingX = prevBounds[4];
      this.keyboardHousingY = prevBounds[5];
      this.keyboardHousingWidth = prevBounds[6];
      this.keyboardHousingHeight = prevBounds[7];
   }

   private void buildLayoutGeometry(List<KeyboardKey> keysWithBindings) {
      KeyboardScreen.SequentialGeometryResult geometry = buildSequentialGeometry(
         this.virtualWidth(),
         this.virtualHeight(),
         this.pixelScale(),
         this.currentGeometryLayout(),
         keysWithBindings,
         this.collectKeysWithAnyBindings(this.allKeys),
         glfw -> this.getFilteredBoxTextLayout(glfw, false),
         this.isDebugMode,
         () -> false
      );
      this.cachedBoxPositions = new LinkedHashMap<>(geometry.boxPositions);
      this.cachedConnectorGeometry = geometry.connectorGeometry;
   }

   private KeyboardLayout currentGeometryLayout() {
      return new KeyboardLayout(
         this.allKeys,
         this.keyboardMinX,
         this.keyboardMaxX,
         this.keyboardMinY,
         this.keyboardMaxY,
         this.keyboardHousingX,
         this.keyboardHousingY,
         this.keyboardHousingWidth,
         this.keyboardHousingHeight,
         this.isCurrentAuxLayoutActive() && this.auxLayout != null ? this.auxLayout.mouseDeviceBounds : null
      );
   }

   private int[] auxToggleButtonBounds(int pixelScale) {
      int centerX = this.virtualWidth() / 2;
      int centerY = this.virtualHeight() / 2;
      int btnW = 12;
      int btnH = Math.max(40, Math.max(1, this.keyboardHousingHeight / Math.max(1, this.pixelScale())));
      int btnXUn = this.auxKeysActive ? 6 : this.virtualWidth() - btnW - 6;
      int btnYUn = centerY - btnH / 2;
      return new int[]{
         OverlayRenderHelper.scaleX(btnXUn, centerX, pixelScale), OverlayRenderHelper.scaleY(btnYUn, centerY, pixelScale), btnW * pixelScale, btnH * pixelScale
      };
   }

   private void prepareVisibleAnimations(long now) {
      if (!this.animationsEnabled()) {
         this.boxAnimStart.clear();
      } else {
         List<ConnectorGeometry> visible = this.frameVisibleGeometry;
         if (!visible.isEmpty()) {
            boolean firstBatch = true;
            int newCount = 0;
            int i = 0;

            for (int n = visible.size(); i < n; i++) {
               int animKey = this.animationKey(visible.get(i).button);
               if (this.boxAnimStart.containsKey(animKey)) {
                  firstBatch = false;
               } else {
                  newCount++;
               }
            }

            if (newCount != 0) {
               i = 0;
               int ix = 0;

               for (int nx = visible.size(); ix < nx; ix++) {
                  ConnectorGeometry geometry = visible.get(ix);
                  int animKey = this.animationKey(geometry.button);
                  if (!this.boxAnimStart.containsKey(animKey)) {
                     this.boxAnimStart.put(animKey, now + (firstBatch ? (long)i * 50000000L : 0L));
                  }

                  i++;
               }
            }
         }
      }
   }

   private void ensureStaticLayerPlan(long now, boolean isAux) {
      List<ConnectorGeometry> visible = this.frameVisibleGeometry;
      int settledCount = 0;
      int i = 0;

      for (int n = visible.size(); i < n; i++) {
         ConnectorGeometry geometry = visible.get(i);
         if (this.isBoxSettled(geometry.button, now) && this.hoverProgress(geometry.button) <= 0.01F) {
            settledCount++;
         }
      }

      List<Integer> prevSettledKeys = isAux ? this.auxStaticSettledBoxKeys : this.staticSettledBoxKeys;
      KeyboardScreen.StaticLayerPlan prevPlan = isAux ? this.auxStaticLayerPlan : this.staticLayerPlan;
      if (settledCount == prevSettledKeys.size() && !prevPlan.isEmpty()) {
         boolean match = true;
         int idx = 0;
         int ix = 0;

         for (int nx = visible.size(); ix < nx && match; ix++) {
            ConnectorGeometry geometry = visible.get(ix);
            if (this.isBoxSettled(geometry.button, now) && this.hoverProgress(geometry.button) <= 0.01F) {
               if (idx >= prevSettledKeys.size() || prevSettledKeys.get(idx) != this.animationKey(geometry.button)) {
                  match = false;
               }

               idx++;
            }
         }

         if (match) {
            return;
         }
      }

      List<Integer> settledKeys = new ArrayList<>(settledCount);
      List<KeyboardScreen.StaticBoxEntry> settledBoxes = new ArrayList<>(settledCount);
      int ix = 0;

      for (int nxx = visible.size(); ix < nxx; ix++) {
         ConnectorGeometry geometry = visible.get(ix);
         if (this.isBoxSettled(geometry.button, now) && !(this.hoverProgress(geometry.button) > 0.01F)) {
            BoxTextLayout layout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, false);
            settledKeys.add(this.animationKey(geometry.button));
            settledBoxes.add(
               new KeyboardScreen.StaticBoxEntry(
                  geometry.boxPos.boxX + geometry.nudgeX, geometry.boxPos.boxY + geometry.nudgeY, layout, this.legendCategoryIdForKey(geometry.button)
               )
            );
         }
      }

      List<KeyboardScreen.StaticKeyEntry> keyEntries = new ArrayList<>(this.allKeys.size());

      for (KeyboardKey button : this.allKeys) {
         boolean hasBinding = this.bindingCache.hasBinding(button.glfwKey);
         keyEntries.add(new KeyboardScreen.StaticKeyEntry(button, hasBinding, this.keyRenderStyle(button, false)));
      }

      int[] mouseDeviceBounds = isAux && this.auxLayout != null ? this.auxLayout.mouseDeviceBounds : null;
      KeyboardScreen.StaticLayerPlan newPlan = new KeyboardScreen.StaticLayerPlan(
         this.keyboardHousingX, this.keyboardHousingY, this.keyboardHousingWidth, this.keyboardHousingHeight, mouseDeviceBounds, keyEntries, settledBoxes
      );
      if (isAux) {
         this.auxStaticLayerPlan = newPlan;
         this.auxStaticSettledBoxKeys = List.copyOf(settledKeys);
      } else {
         this.staticLayerPlan = newPlan;
         this.staticSettledBoxKeys = List.copyOf(settledKeys);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalScroll, double verticalScroll) {
      float s = this.responsiveScale();
      mouseX /= (double) s;
      mouseY /= (double) s;
      if (this.hiddenKeysModeActive && this.categoryPanel != null && this.categoryPanel.isPointOverPanel((int)mouseX, (int)mouseY, this.categoryPanelEntries())
         )
       {
         int dir = verticalScroll > 0.0 ? -1 : 1;
         this.categoryPanel.scroll(dir, this.categoryPanelEntries());
         return true;
      } else if (this.editModeState.isActive()
         && this.unassignedPanel != null
         && this.unassignedPanel.panelProgress() > 0.0F
         && this.unassignedPanel.isPointOverPanel((int)mouseX, (int)mouseY, this.unassignedByMod)) {
         int dir = (int)Math.signum(verticalScroll);
         this.unassignedPanel.scroll(dir);
         return true;
      } else {
         if (this.modFilterOpen
            && this.cachedLegendOverlay != null
            && this.cachedLegendOverlay
               .isPointOverModFilter((int)mouseX, (int)mouseY, this.font, this.modFilterOpen, this.easeInOut(this.modFilterPanelProgress))
            && this.modSources != null
            && !this.modSources.isEmpty()) {
            int totalRows = KeyboardLegendOverlay.displayRowCount(this.modSources, this.modExpanded, this.bindingsByMod, this.modExpandProgress);
            int visible = 8;
            int maxStart = Math.max(0, totalRows - visible);
            if (maxStart > 0) {
               int dir = (int)Math.signum(verticalScroll);
               this.modPanelScroll = Math.max(0, Math.min(maxStart, this.modPanelScroll - dir));
               return true;
            }
         }

         return super.mouseScrolled(mouseX, mouseY, horizontalScroll, verticalScroll);
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      float s = this.responsiveScale();
      mouseX /= (double) s;
      mouseY /= (double) s;
      dragX /= (double) s;
      dragY /= (double) s;
      if (button == 0) {
         if (this.editModeState.isActive() && this.unassignedPanel != null && this.unassignedPanel.dragScrollbarTo((int)mouseY, this.unassignedByMod)) {
            return true;
         }

         if (!this.isDebugMode && this.cachedLegendOverlay != null && this.font != null) {
            int updatedScroll = this.cachedLegendOverlay
               .dragScrollbarTo(
                  (int)mouseY,
                  this.font,
                  this.modSources,
                  this.modExpanded,
                  this.bindingsByMod,
                  this.modExpandProgress,
                  this.modPanelScroll,
                  this.easeInOut(this.modFilterPanelProgress)
               );
            if (this.cachedLegendOverlay.isDraggingScrollbar()) {
               this.modPanelScroll = updatedScroll;
               return true;
            }
         }
      }

      return false;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      float s = this.responsiveScale();
      mouseX /= (double) s;
      mouseY /= (double) s;
      boolean handled = false;
      if (button == 0) {
         if (this.unassignedPanel != null && this.unassignedPanel.isDraggingScrollbar()) {
            this.unassignedPanel.endScrollbarDrag();
            handled = true;
         }

         if (this.cachedLegendOverlay != null && this.cachedLegendOverlay.isDraggingScrollbar()) {
            this.cachedLegendOverlay.endScrollbarDrag();
            handled = true;
         }
      }

      return handled;
   }

   private void renderHoveredKeys(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer) {
      boolean anyMouseHovered = false;

      for (KeyboardKey button : this.allKeys) {
         boolean hasBinding = this.bindingCache.hasBinding(button.glfwKey) || this.bindingCache.hasBindingAnyLayer(button.glfwKey);
         if (hasBinding && !this.isBindingHidden(button) && this.isButtonHovered(button)) {
            if (button.isMouseKey) {
               anyMouseHovered = true;
            } else {
               renderer.drawKey(guiGraphics, button, this.keyRenderStyle(button, true), true, this.legendAlphaForKey(button));
            }
         }
      }

      if (anyMouseHovered) {
         KeyboardLayout activeLayout = this.auxKeysActive ? this.auxLayout : null;
         if (activeLayout != null && activeLayout.mouseDeviceBounds != null) {
            List<KeyboardKey> mouseKeys = new ArrayList<>();

            for (KeyboardKey k : this.allKeys) {
               if (k.isMouseKey) {
                  mouseKeys.add(k);
               }
            }

            int[] mb = activeLayout.mouseDeviceBounds;
            renderer.drawMouseDevice(
               guiGraphics,
               mb[0],
               mb[1],
               mb[2],
               mb[3],
               mouseKeys,
               kx -> this.fadedMouseStyle(this.keyRenderStyle(kx, this.isButtonHovered(kx)), this.legendAlphaForKey(kx))
            );
         }
      }
   }

   private void renderConnectorLines(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, long now) {
      List<ConnectorGeometry> visible = this.frameVisibleGeometry;
      if (!visible.isEmpty()) {
         boolean alternatingLineColors = KeybindAtlasClientConfig.alternatingLineColorsEnabled();
         renderer.beginPolyLineBatch(guiGraphics);
         int i = 0;

         for (int n = visible.size(); i < n; i++) {
            ConnectorGeometry geometry = visible.get(i);
            boolean hovered = this.isButtonHovered(geometry.button);
            float elapsedMs = this.getBoxElapsedMs(geometry.button, now);
            float lineProgress = this.easeOut(Math.min(1.0F, Math.max(0.0F, (elapsedMs - 350.0F) / 220.0F)));
            float alpha = this.legendAlphaForKey(geometry.button);
            int lineColor = hovered
               ? KeyVisualStyle.lineHover()
               : (alternatingLineColors && geometry.useDarkColor ? KeyVisualStyle.lineDefaultAlt() : KeyVisualStyle.lineDefault());
            renderer.drawPolyLineProgress(
               guiGraphics, geometry.waypoints, OverlayRenderHelper.withAlpha(lineColor, alpha), lineProgress, geometry.totalLineLength
            );
         }

         renderer.endPolyLineBatch();
      }
   }

   private void renderDynamicBoxes(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, long now) {
      int slidePixels = 8 * this.pixelScale();
      this.deferredHoveredEntry = null;
      KeyboardScreen.DynamicBoxEntry hoveredEntry = null;
      List<ConnectorGeometry> visible = this.frameVisibleGeometry;
      int i = 0;

      for (int n = visible.size(); i < n; i++) {
         ConnectorGeometry geometry = visible.get(i);
         BoxTextLayout compactLayout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, false);
         float legendAlpha = this.legendAlphaForKey(geometry.button);
         boolean hovered = this.isButtonHovered(geometry.button);
         float hoverProgress = this.hoverProgress(geometry.button);
         float hoverVisualProgress = this.hoverVisualProgress(geometry.button, hoverProgress, hovered);
         float elapsedMs = this.getBoxElapsedMs(geometry.button, now);
         boolean settled = elapsedMs >= 570.0F;
         if (!settled || !(hoverProgress <= 0.01F)) {
            float boxAlpha = this.easeOut(Math.min(1.0F, elapsedMs / 350.0F));
            float baseTextAlpha = this.easeOut(Math.min(1.0F, Math.max(0.0F, (elapsedMs - 350.0F) / 200.0F)));
            int slideX = geometry.boxLeft
               ? Math.round((1.0F - boxAlpha) * (float)slidePixels)
               : (geometry.boxRight ? -Math.round((1.0F - boxAlpha) * (float)slidePixels) : 0);
            int slideY = geometry.boxAbove
               ? Math.round((1.0F - boxAlpha) * (float)slidePixels)
               : (geometry.boxBelow ? -Math.round((1.0F - boxAlpha) * (float)slidePixels) : 0);
            BoxTextLayout expandedLayout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, true);
            BoxTextLayout layout = hoverVisualProgress <= 0.01F
               ? compactLayout
               : (hoverVisualProgress >= 0.99F ? expandedLayout : this.interpolateHoverLayout(compactLayout, expandedLayout, hoverVisualProgress));
            KeyboardScreen.StaticBoxEntry placement = this.resolveBoxPlacement(geometry, layout);
            int drawX = placement.x + slideX;
            int drawY = placement.y + slideY;
            float textHoverAlpha;
            if (hoverVisualProgress <= 0.01F) {
               textHoverAlpha = 1.0F;
            } else if (!hovered) {
               textHoverAlpha = Math.max(0.0F, hoverVisualProgress * 2.5F - 1.5F);
               textHoverAlpha *= textHoverAlpha;
            } else if (hoverVisualProgress >= 0.99F) {
               textHoverAlpha = 1.0F;
            } else {
               textHoverAlpha = Math.max(0.0F, hoverVisualProgress * 2.5F - 1.5F);
               textHoverAlpha *= textHoverAlpha;
            }

            float textAlpha = baseTextAlpha * textHoverAlpha * legendAlpha;
            if (hovered) {
               hoveredEntry = new KeyboardScreen.DynamicBoxEntry(drawX, drawY, layout, true, boxAlpha * legendAlpha, textAlpha);
            } else if (hoverProgress > 0.01F) {
               renderer.drawDynamicBox(guiGraphics, drawX, drawY, layout, false, boxAlpha * legendAlpha, textAlpha);
            } else {
               renderer.drawDynamicBox(guiGraphics, drawX, drawY, layout, false, boxAlpha * legendAlpha, textAlpha);
            }
         }
      }

      this.deferredHoveredEntry = hoveredEntry;
   }

   private void drawEditButtons(GuiGraphicsExtractor guiGraphics, KeyboardRenderer renderer, int boxX, int boxY, BoxTextLayout layout) {
      int scale = this.pixelScale();
      int boxTextColor = KeyVisualStyle.boxText();
      this.editPenHoveredIndex = -1;
      int assignIndex = 0;
      int btnPad = 2 * scale;

      for (BoxTextRow row : layout.rows) {
         if (row.kind == BoxTextRow.Kind.TEXT && row.color == boxTextColor) {
            int btnH = Math.min(row.height, row.height);
            int btnX = boxX + layout.dimensions.width - btnH - btnPad;
            int btnY = boxY + row.yOffset;
            boolean btnHovered = OverlayRenderHelper.isMouseOver(this.hoveredKeyX, this.hoveredKeyY, btnX, btnY, btnH, btnH, scale);
            if (btnHovered) {
               this.editPenHoveredIndex = assignIndex;
            }

            int borderColor = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), btnHovered ? 1.0F : 0.5F);
            int bgColor = OverlayRenderHelper.withAlpha(btnHovered ? KeyVisualStyle.boxHoverFill() : KeyVisualStyle.boxFill(), 0.9F);
            guiGraphics.fill(btnX, btnY, btnX + btnH, btnY + btnH, bgColor);
            OverlayRenderHelper.drawRectOutline(guiGraphics, btnX, btnY, btnH, btnH, borderColor, scale);
            assignIndex++;
         }
      }
   }

   private String getAssignmentLabelAtPenIndex(int glfwKey, int penIndex) {
      BoxTextLayout layout = this.getFilteredBoxTextLayout(glfwKey, true);
      int boxTextColor = KeyVisualStyle.boxText();
      int idx = 0;

      for (BoxTextRow row : layout.rows) {
         if (row.kind == BoxTextRow.Kind.TEXT && row.color == boxTextColor) {
            if (idx == penIndex) {
               return row.text;
            }

            idx++;
         }
      }

      return null;
   }

   private void renderDebugGeometry(GuiGraphicsExtractor guiGraphics) {
      if (this.isDebugMode) {
         ConnectorGeometry focused = this.focusedDebugGeometry();
         if (focused != null) {
            int scale = this.pixelScale();

            for (ConnectorGeometry geometry : this.cachedConnectorGeometry) {
               if (this.isBindingVisible(geometry)) {
                  int color = geometry == focused ? -797349 : 1726179946;
                  OverlayRenderHelper.drawRectOutline(
                     guiGraphics,
                     geometry.boxPos.boxX + geometry.nudgeX,
                     geometry.boxPos.boxY + geometry.nudgeY,
                     geometry.boxPos.boxWidth,
                     geometry.boxPos.boxHeight,
                     color,
                     scale
                  );
               }
            }

            OverlayRenderHelper.drawRectOutline(guiGraphics, focused.button.x, focused.button.y, focused.button.width, focused.button.height, -10759181, scale);

            for (int i = 0; i + 1 < focused.waypoints.length; i += 2) {
               int x = focused.waypoints[i];
               int y = focused.waypoints[i + 1];
               int size = Math.max(3 * scale, 4);
               guiGraphics.fill(x - size / 2, y - size / 2, x + (size + 1) / 2, y + (size + 1) / 2, -2893);
               this.drawDebugLabel(guiGraphics, Integer.toString(i / 2), x + 2 * scale, y - 4 * scale, -2893, 0.55F);
            }

            BoxTextLayout compactLayout = this.getFilteredBoxTextLayout(focused.button.glfwKey, false);
            int boxX = focused.boxPos.boxX + focused.nudgeX;
            int boxY = focused.boxPos.boxY + focused.nudgeY;
            List<String> lines = new ArrayList<>();
            lines.add("DBG " + (focused.bindingIndex + 1) + " " + focused.button.label);
            lines.add("screen " + this.screenResolutionString());
            lines.add("key  " + this.rectString(focused.button.x, focused.button.y, focused.button.width, focused.button.height));
            lines.add("box  " + this.rectString(boxX, boxY, focused.boxPos.boxWidth, focused.boxPos.boxHeight));
            lines.add("end  (" + focused.boxPos.lineEndX + "," + focused.boxPos.lineEndY + ") nudge(" + focused.nudgeX + "," + focused.nudgeY + ")");
            lines.add("text " + compactLayout.dimensions.width + "x" + compactLayout.dimensions.height);
            lines.add("path " + this.waypointString(focused.waypoints));
            this.drawDebugPanel(guiGraphics, lines);
         }
      }
   }

   private ConnectorGeometry focusedDebugGeometry() {
      List<ConnectorGeometry> activeGeometry = this.activeConnectorGeometryList();
      if (activeGeometry.isEmpty()) {
         return null;
      } else {
         if (this.debugIndex >= 0) {
            for (ConnectorGeometry geometry : activeGeometry) {
               if (geometry.bindingIndex == this.debugIndex) {
                  return geometry;
               }
            }
         }

         if (this.hoveredBoxKey != null) {
            for (ConnectorGeometry geometryx : activeGeometry) {
               if (geometryx.button == this.hoveredBoxKey) {
                  return geometryx;
               }
            }
         }

         for (ConnectorGeometry geometryxx : activeGeometry) {
            if (this.isBindingVisible(geometryxx)) {
               return geometryxx;
            }
         }

         return null;
      }
   }

   private void drawDebugPanel(GuiGraphicsExtractor guiGraphics, List<String> lines) {
      if (!lines.isEmpty()) {
         int scale = this.pixelScale();
         int maxWidth = 0;

         for (String line : lines) {
            maxWidth = Math.max(maxWidth, Math.round((float)this.font.width(line) * 0.8F));
         }

         int textHeight = Math.max(1, Math.round(9.0F * 0.8F));
         int width = (8 + maxWidth) * scale;
         int height = (8 + lines.size() * textHeight + Math.max(0, lines.size() - 1) * 2) * scale;
         int x = this.sx(6);
         int y = this.sy(this.virtualHeight() - (8 + lines.size() * textHeight + Math.max(0, lines.size() - 1) * 2) - 6);
         guiGraphics.fill(x, y, x + width, y + height, OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), 0.8F));
         OverlayRenderHelper.drawRectOutline(guiGraphics, x, y, width, height, KeyVisualStyle.boxBorder(), scale);

         for (int i = 0; i < lines.size(); i++) {
            int lineY = y + (4 + i * (textHeight + 2)) * scale;
            this.drawDebugLabel(guiGraphics, lines.get(i), x + 4 * scale, lineY, KeyVisualStyle.boxText(), 0.8F);
         }
      }
   }

   private void drawDebugLabel(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int color, float scaleMultiplier) {
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate((float)((float)x), (float)((float)y));
      float labelScale = (float)this.pixelScale() * scaleMultiplier;
      guiGraphics.pose().scale((float)(labelScale), (float)(labelScale));
      guiGraphics.text(this.font, text, 0, 0, color, false);
      guiGraphics.pose().popMatrix();
   }

   private int sx(int x) {
      return OverlayRenderHelper.scaleX(x, this.virtualWidth() / 2, this.pixelScale());
   }

   private int sy(int y) {
      return OverlayRenderHelper.scaleY(y, this.virtualHeight() / 2, this.pixelScale());
   }

   private String screenResolutionString() {
      if (this.minecraft == null) {
         return this.width + "x" + this.height + " gui @" + this.pixelScale() + "x";
      } else {
         int windowWidth = this.minecraft.getWindow().getWidth();
         int windowHeight = this.minecraft.getWindow().getHeight();
         return windowWidth + "x" + windowHeight + " win, " + this.width + "x" + this.height + " gui @" + this.pixelScale() + "x";
      }
   }

   private Path debugLogDirectory() {
      return this.minecraft == null ? Path.of("keybindatlas-debug") : this.minecraft.gameDirectory.toPath().resolve("keybindatlas-debug");
   }

   private void dumpDebugLog() {
      this.ensureBindingLayoutCache();
      Path directory = this.debugLogDirectory();

      try {
         Files.createDirectories(directory);
         String fileName = "keybindatlas-debug-" + LocalDateTime.now().format(DEBUG_LOG_TIMESTAMP) + ".txt";
         Path file = directory.resolve(fileName);
         Files.writeString(file, this.buildDebugLog(), StandardCharsets.UTF_8);
         this.pruneOldDebugLogs(directory, 5);
         this.showStatusMessage(this.debugOverlayMessage("ui.status.debug_log_saved", file.getFileName().toString()));
      } catch (IOException var4) {
         this.showStatusMessage(this.debugOverlayMessage("ui.status.debug_log_failed"));
      }
   }

   private void pruneOldDebugLogs(Path directory, int maxLogs) {
      if (maxLogs > 0) {
         try (Stream<Path> files = Files.list(directory)) {
            List<Path> logFiles = files.filter(path -> {
               String name = path.getFileName().toString();
               return name.startsWith("keybindatlas-debug-") && name.endsWith(".txt");
            }).sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString())).toList();

            for (int index = maxLogs; index < logFiles.size(); index++) {
               Files.deleteIfExists(logFiles.get(index));
            }
         } catch (IOException var8) {
         }
      }
   }

   private void openDebugLogFolder() {
      Path directory = this.debugLogDirectory();

      try {
         Files.createDirectories(directory);
         Util.getPlatform().openFile(directory.toFile());
         this.showStatusMessage(this.debugOverlayMessage("ui.status.debug_folder_opened"));
      } catch (IOException var3) {
         this.showStatusMessage(this.debugOverlayMessage("ui.status.debug_folder_failed"));
      }
   }

   private Component debugOverlayMessage(String path, Object... args) {
      return AtlasText.translatable(path, args).withStyle(Style.EMPTY.withColor(12436428));
   }

   private String buildDebugLog() {
      String lineSep = System.lineSeparator();
      StringBuilder out = new StringBuilder();
      List<ConnectorGeometry> debugLogGeometry = new ArrayList<>(this.buildDebugLogGeometry());
      debugLogGeometry.sort((a, b) -> Integer.compare(a.bindingIndex, b.bindingIndex));
      out.append("Keybind Atlas Debug Log").append(lineSep);
      out.append("screen=").append(this.screenResolutionString()).append(lineSep);
      out.append("debugMode=").append(this.isDebugMode).append(lineSep);
      out.append("animationsEnabled=").append(this.animationsEnabled()).append(lineSep);
      out.append("hiddenBindingsEnabled=").append(this.hiddenBindingsEnabled()).append(lineSep);
      out.append("activeKeyboard=").append(this.auxKeysActive ? "extra" : "main").append(lineSep);
      out.append("visibleBindingBoxes=").append(this.currentVisibleBindingBoxCount()).append(lineSep);
      out.append("keyboardBounds=")
         .append(this.rectString(this.keyboardHousingX, this.keyboardHousingY, this.keyboardHousingWidth, this.keyboardHousingHeight))
         .append(lineSep);
      out.append("configuredHiddenLabels=").append(KeybindAtlasClientConfig.configuredHiddenBindingLabels()).append(lineSep);
      out.append("routingAlgorithm=orthogonal-a-star").append(lineSep);
      out.append("routingGridStep=").append(this.pixelScale()).append(lineSep);
      out.append("routingFallback=line-cross-only").append(lineSep);
      out.append("routeTraceSource=").append(debugLogGeometry == this.cachedConnectorGeometry ? "cached" : "rebuilt-debug").append(lineSep);
      out.append(lineSep);

      for (int i = 0; i < debugLogGeometry.size(); i++) {
         ConnectorGeometry geometry = debugLogGeometry.get(i);
         BoxTextLayout compactLayout = this.getFilteredBoxTextLayout(geometry.button.glfwKey, false);
         int boxX = geometry.boxPos.boxX + geometry.nudgeX;
         int boxY = geometry.boxPos.boxY + geometry.nudgeY;
         out.append("[").append(i + 1).append("] ").append(geometry.button.label).append(lineSep);
         out.append("glfw=").append(geometry.button.glfwKey).append(lineSep);
         out.append("bindingIndex=").append(geometry.bindingIndex).append(lineSep);
         out.append("hiddenBinding=").append(KeybindAtlasClientConfig.isHiddenBindingForKey(geometry.button.label, geometry.button.glfwKey)).append(lineSep);
         out.append("key=").append(this.rectString(geometry.button.x, geometry.button.y, geometry.button.width, geometry.button.height)).append(lineSep);
         out.append("box=").append(this.rectString(boxX, boxY, geometry.boxPos.boxWidth, geometry.boxPos.boxHeight)).append(lineSep);
         out.append("boxSide=").append(this.debugBoxSideString(geometry)).append(lineSep);
         out.append("lineEnd=(").append(geometry.boxPos.lineEndX).append(",").append(geometry.boxPos.lineEndY).append(")").append(lineSep);
         out.append("nudge=(").append(geometry.nudgeX).append(",").append(geometry.nudgeY).append(")").append(lineSep);
         out.append("text=").append(compactLayout.dimensions.width).append("x").append(compactLayout.dimensions.height).append(lineSep);
         out.append("routeMode=").append(geometry.routeMode).append(lineSep);
         out.append("routeStart=").append(this.routeStartString(geometry.waypoints)).append(lineSep);
         out.append("routeEnd=").append(this.routeEndString(geometry.waypoints)).append(lineSep);
         out.append("routeLength=").append(geometry.totalLineLength).append(lineSep);
         out.append("routeTurns=").append(this.routeTurnCount(geometry.waypoints)).append(lineSep);
         out.append("path=").append(this.waypointString(geometry.waypoints)).append(lineSep);
         if (!geometry.routeDebug.isEmpty()) {
            out.append("routeDebug=").append(geometry.routeDebug).append(lineSep);
         }

         out.append(lineSep);
      }

      return out.toString();
   }

   private List<ConnectorGeometry> buildDebugLogGeometry() {
      if (!this.cachedConnectorGeometry.isEmpty() && !this.cachedBoxPositions.isEmpty()) {
         List<KeyboardKey> visibleKeys = this.collectKeysWithBindings(this.allKeys);
         if (visibleKeys.isEmpty()) {
            return this.cachedConnectorGeometry;
         } else {
            KeyboardScreen.SequentialGeometryResult geometry = buildSequentialGeometry(
               this.virtualWidth(),
               this.virtualHeight(),
               this.pixelScale(),
               this.currentGeometryLayout(),
               visibleKeys,
               this.collectKeysWithAnyBindings(this.allKeys),
               glfw -> this.getFilteredBoxTextLayout(glfw, false),
               true,
               () -> false
            );
            List<ConnectorGeometry> debugGeometry = geometry.connectorGeometry;
            return debugGeometry.size() != this.cachedConnectorGeometry.size() ? this.cachedConnectorGeometry : debugGeometry;
         }
      } else {
         return this.cachedConnectorGeometry;
      }
   }

   private String debugBoxSideString(ConnectorGeometry geometry) {
      if (geometry.boxAbove) {
         return "top";
      } else if (geometry.boxBelow) {
         return "bottom";
      } else if (geometry.boxLeft) {
         return "left";
      } else {
         return geometry.boxRight ? "right" : "overlap";
      }
   }

   private String routeStartString(int[] waypoints) {
      return waypoints.length < 2 ? "()" : this.pointString(waypoints[0], waypoints[1]);
   }

   private String routeEndString(int[] waypoints) {
      return waypoints.length < 2 ? "()" : this.pointString(waypoints[waypoints.length - 2], waypoints[waypoints.length - 1]);
   }

   private int routeTurnCount(int[] waypoints) {
      return Math.max(0, waypoints.length / 2 - 2);
   }

   private String pointString(int x, int y) {
      return "(" + x + "," + y + ")";
   }

   private String rectString(int x, int y, int width, int height) {
      return x + "," + y + " " + width + "x" + height;
   }

   private String waypointString(int[] waypoints) {
      StringBuilder builder = new StringBuilder();

      for (int i = 0; i + 1 < waypoints.length; i += 2) {
         if (i > 0) {
            builder.append(" -> ");
         }

         builder.append('(').append(waypoints[i]).append(',').append(waypoints[i + 1]).append(')');
      }

      return builder.toString();
   }

   private boolean isBoxSettled(KeyboardKey button, long now) {
      return this.getBoxElapsedMs(button, now) >= 570.0F;
   }

   private boolean isBindingVisible(ConnectorGeometry geometry) {
      boolean debugVisible = !this.isDebugMode || this.debugIndex >= 0 && geometry.bindingIndex <= this.debugIndex;
      return debugVisible && this.hasVisibleAssignments(geometry.button.glfwKey);
   }

   private List<ConnectorGeometry> computeFrameVisibleGeometry(List<ConnectorGeometry> geometries) {
      List<ConnectorGeometry> result = new ArrayList<>();

      for (ConnectorGeometry geometry : geometries) {
         if (this.isBindingVisible(geometry) && !this.getFilteredBoxTextLayout(geometry.button.glfwKey, false).rows.isEmpty()) {
            result.add(geometry);
         }
      }

      return result;
   }

   private boolean isButtonHovered(KeyboardKey button) {
      return button.isHovered(this.hoveredKeyX, this.hoveredKeyY, this.pixelScale())
         || this.hoveredBoxKey != null && button.glfwKey == this.hoveredBoxKey.glfwKey;
   }

   private BoxTextLayout getFilteredBoxTextLayout(int glfwKey, boolean hovered) {
      return this.getFilteredBoxTextLayout(glfwKey, hovered, this.currentVisibleBindingBoxCount());
   }

   private BoxTextLayout getFilteredBoxTextLayout(int glfwKey, boolean hovered, int visibleBindingBoxCount) {
      List<KeyAssignmentInfo> assigns = this.bindingCache.assignmentsForKey(glfwKey);
      boolean editModeHovered = this.editModeState.isActive() && hovered;
      if (assigns == null || assigns.isEmpty()) {
         return this.bindingCache.getBoxTextLayout(glfwKey, hovered, editModeHovered, visibleBindingBoxCount, this.cachedLayoutFactory);
      } else if (!this.assignmentFiltersActive) {
         return this.bindingCache.getBoxTextLayout(glfwKey, hovered, editModeHovered, visibleBindingBoxCount, this.cachedLayoutFactory);
      } else {
         KeyboardScreen.FilteredLayoutKey cacheKey = new KeyboardScreen.FilteredLayoutKey(glfwKey, hovered, editModeHovered, visibleBindingBoxCount);
         return this.filteredLayoutCache.computeIfAbsent(cacheKey, ignored -> {
            List<KeyAssignmentInfo> filtered = new ArrayList<>();

            for (KeyAssignmentInfo assignment : assigns) {
               if (this.isAssignmentVisible(assignment)) {
                  filtered.add(assignment);
               }
            }

            return this.cachedLayoutFactory.create(filtered, hovered, editModeHovered, visibleBindingBoxCount);
         });
      }
   }

   private void persistDisabledBindings() {
      List<String> disabled = new ArrayList<>();

      for (Entry<String, Map<String, Boolean>> modEntry : this.bindingEnabled.entrySet()) {
         String src = modEntry.getKey();

         for (Entry<String, Boolean> bindEntry : modEntry.getValue().entrySet()) {
            if (!Boolean.TRUE.equals(bindEntry.getValue())) {
               disabled.add(src + "::" + bindEntry.getKey());
            }
         }
      }

      KeybindAtlasClientConfig.setDisabledBindings(disabled);
   }

   private int animationKey(KeyboardKey button) {
      return button.glfwKey;
   }

   private float getBoxElapsedMs(KeyboardKey button, long now) {
      if (!this.animationsEnabled()) {
         return 570.0F;
      } else {
         Long start = this.boxAnimStart.get(this.animationKey(button));
         if (start == null) {
            return 0.0F;
         } else {
            long deltaNanos = Math.max(0L, now - start);
            return (float)deltaNanos / 1000000.0F;
         }
      }
   }

   private float easeOut(float t) {
      return 1.0F - (1.0F - t) * (1.0F - t);
   }

   private float easeInOut(float t) {
      return t < 0.5F ? 2.0F * t * t : 1.0F - (-2.0F * t + 2.0F) * (-2.0F * t + 2.0F) / 2.0F;
   }

   private Set<Integer> buildVisibleAnimationKeys() {
      Set<Integer> cached = this.cachedVisibleAnimationKeys;
      if (cached != null) {
         return cached;
      } else {
         Set<Integer> keys = new HashSet<>();

         for (KeyboardKey b : this.allKeys) {
            if (this.bindingCache.hasBinding(b.glfwKey) && !this.isBindingHidden(b) && this.hasVisibleAssignments(b.glfwKey)) {
               keys.add(b.glfwKey);
            }
         }

         this.cachedVisibleAnimationKeys = keys;
         return keys;
      }
   }

   private void updatePanelAnimations(long now) {
      float deltaMs = this.lastPanelAnimNanos < 0L ? 16.0F : (float)(now - this.lastPanelAnimNanos) / 1000000.0F;
      this.lastPanelAnimNanos = now;
      float settingsStep = deltaMs / 264.0F;
      float modFilterStep = deltaMs / 150.0F;
      float categoryModeHudStep = deltaMs / 126.0F;
      float editToolbarStep = deltaMs / 150.0F;
      float modExpandStep = deltaMs / 90.0F;
      float unassignedStep = deltaMs / 177.0F;
      float categoryStep = deltaMs / 231.0F;
      float settingsTarget = this.settingsOpen ? 1.0F : 0.0F;
      this.settingsPanelProgress = this.moveTowards(this.settingsPanelProgress, settingsTarget, settingsStep);
      float modTarget = this.modFilterOpen ? 1.0F : 0.0F;
      this.modFilterPanelProgress = this.moveTowards(this.modFilterPanelProgress, modTarget, modFilterStep);
      float categoryModeHudTarget = this.hiddenKeysModeActive ? 1.0F : 0.0F;
      this.categoryModeHudProgress = this.moveTowards(this.categoryModeHudProgress, categoryModeHudTarget, categoryModeHudStep);
      if (this.cachedEditModeOverlay != null) {
         this.cachedEditModeOverlay.updateAnimations(editToolbarStep, this.editModeState.isActive());
      }

      int i = 0;

      for (int n = this.modSources.size(); i < n; i++) {
         String mod = this.modSources.get(i);
         float target = Boolean.TRUE.equals(this.modExpanded.get(mod)) ? 1.0F : 0.0F;
         float current = this.modExpandProgress.getOrDefault(mod, 0.0F);
         if (current != target) {
            float next = this.moveTowards(current, target, modExpandStep);
            this.modExpandProgress.put(mod, next);
         }
      }

      if (this.unassignedPanel != null) {
         this.unassignedPanel.updateAnimations(unassignedStep, this.unassignedByMod);
      }

      if (this.categoryPanel != null) {
         this.categoryPanel.updateAnimations(categoryStep);
      }
   }

   private void updateHoverAnimations(long now) {
      if (!this.animationsEnabled()) {
         this.applyInstantHoverState();
         this.lastHoverAnimationNanos = now;
      } else {
         float deltaMs = this.lastHoverAnimationNanos < 0L ? 105.0F : (float)(now - this.lastHoverAnimationNanos) / 1000000.0F;
         this.lastHoverAnimationNanos = now;
         Set<Integer> visibleKeys = this.buildVisibleAnimationKeys();
         this.hoverProgressByKey.keySet().removeIf(key -> !visibleKeys.contains(key));

         for (KeyboardKey button : this.allKeys) {
            if (this.bindingCache.hasBinding(button.glfwKey)) {
               float current = this.hoverProgressByKey.getOrDefault(button.glfwKey, 0.0F);
               float target = this.isButtonHovered(button) ? 1.0F : 0.0F;
               float durationMs = this.hoverTransitionDurationMs(button);
               float step = Math.min(1.0F, deltaMs / durationMs);
               float next = this.moveTowards(current, target, step);
               if (next <= 0.001F && target <= 0.0F) {
                  this.hoverProgressByKey.remove(button.glfwKey);
               } else {
                  this.hoverProgressByKey.put(button.glfwKey, next);
               }
            }
         }
      }
   }

   private float hoverProgress(KeyboardKey button) {
      return this.hoverProgressByKey.getOrDefault(button.glfwKey, 0.0F);
   }

   private void applyInstantHoverState() {
      Set<Integer> visibleKeys = this.buildVisibleAnimationKeys();
      this.hoverProgressByKey.keySet().removeIf(key -> !visibleKeys.contains(key));

      for (KeyboardKey button : this.allKeys) {
         if (this.bindingCache.hasBinding(button.glfwKey)) {
            if (this.isButtonHovered(button)) {
               this.hoverProgressByKey.put(button.glfwKey, 1.0F);
            } else {
               this.hoverProgressByKey.remove(button.glfwKey);
            }
         }
      }
   }

   private void resetAnimationState() {
      this.boxAnimStart.clear();
      this.hoverProgressByKey.clear();
      this.lastHoverAnimationNanos = -1L;
      this.markStaticLayerDirty();
   }

   private float moveTowards(float current, float target, float amount) {
      return current < target ? Math.min(target, current + amount) : Math.max(target, current - amount);
   }

   private float hoverVisualProgress(KeyboardKey button, float progress, boolean hovered) {
      if (hovered) {
         return this.easeOut(progress);
      } else {
         float inverse = 1.0F - progress;
         return 1.0F - this.easeOut(inverse);
      }
   }

   private float hoverTransitionDurationMs(KeyboardKey button) {
      BoxTextLayout compactLayout = this.getFilteredBoxTextLayout(button.glfwKey, false);
      BoxTextLayout expandedLayout = this.getFilteredBoxTextLayout(button.glfwKey, true);
      int growth = Math.max(0, expandedLayout.dimensions.width - compactLayout.dimensions.width)
         + Math.max(0, expandedLayout.dimensions.height - compactLayout.dimensions.height);
      return Math.min(220.0F, 105.0F + (float)growth * 0.9F);
   }

   private BoxTextLayout interpolateHoverLayout(BoxTextLayout compactLayout, BoxTextLayout expandedLayout, float progress) {
      int width = this.lerpInt(compactLayout.dimensions.width, expandedLayout.dimensions.width, progress);
      int height = this.lerpInt(compactLayout.dimensions.height, expandedLayout.dimensions.height, progress);
      return new BoxTextLayout(expandedLayout.rows, new BoxDimensions(width, height));
   }

   private int lerpInt(int start, int end, float progress) {
      return Math.round((float)start + (float)(end - start) * progress);
   }

   private List<ConnectorGeometry> activeConnectorGeometryList() {
      return this.auxKeysActive ? this.cachedAuxConnectorGeometry : this.cachedConnectorGeometry;
   }

   private void setHiddenKeysModeActive(boolean active) {
      this.hiddenKeysModeActive = active;
      if (this.categoryPanel != null && !active) {
         this.categoryPanel.setExpanded(false);
      }

      if (!active) {
         this.categoryInfoVisible = false;
      }

      if (!active && this.categoryNameDialog != null && this.categoryNameDialog.isVisible()) {
         this.categoryNameDialog.close();
      }
   }

   private void toggleCategoryInfoVisible() {
      if (this.hiddenKeysModeActive) {
         this.categoryInfoVisible = !this.categoryInfoVisible;
      }
   }

   private void toggleHiddenKeysMode() {
      boolean next = !this.hiddenKeysModeActive;
      if (next) {
         this.collapseOtherMenusForCategoryMode();
         this.setHiddenKeysModeActive(true);
         this.categoryInfoVisible = true;
         if (this.categoryPanel != null) {
            this.categoryPanel.setExpanded(true);
         }

         this.selectCategory(this.selectedCategoryId);
      } else {
         this.closeCategoryMode();
      }

      this.showStatusMessage(HIDDEN_KEYS_STATUS_MESSAGE);
   }

   private List<KeyboardKey> activeKeyboardKeysForInput() {
      if (this.auxKeysActive) {
         this.ensureAuxLayout();
         if (this.auxLayout != null) {
            return this.auxLayout.keys;
         }
      }

      return this.allKeys;
   }

   private KeyboardKey hiddenBindingKeyAtMouse(int mouseX, int mouseY) {
      if (this.isPointOverAnyOpenPanel(mouseX, mouseY)) {
         return null;
      } else {
         int scaledMouseX = OverlayRenderHelper.scaleX(mouseX, this.virtualWidth() / 2, this.pixelScale());
         int scaledMouseY = OverlayRenderHelper.scaleY(mouseY, this.virtualHeight() / 2, this.pixelScale());
         List<KeyboardKey> activeKeys = this.activeKeyboardKeysForInput();

         for (int index = activeKeys.size() - 1; index >= 0; index--) {
            KeyboardKey key = activeKeys.get(index);
            if (this.bindingCache.hasBinding(key.glfwKey) && key.isHovered(scaledMouseX, scaledMouseY, this.pixelScale())) {
               return key;
            }
         }

         return null;
      }
   }

   private void toggleHiddenBindingForKey(KeyboardKey key) {
      if (key != null) {
         if (!KeybindAtlasClientConfig.hiddenBindingsEnabled()) {
            KeybindAtlasClientConfig.setHiddenBindingsEnabled(true);
         }

         KeybindAtlasClientConfig.toggleHiddenBindingLabel(key.label, key.glfwKey);
         this.markBindingLayoutDirty();
      }
   }

   private KeyboardKey debugKeyAtIndex(int index) {
      if (index < 0) {
         return null;
      } else {
         for (ConnectorGeometry geometry : this.activeConnectorGeometryList()) {
            if (geometry.bindingIndex == index) {
               return geometry.button;
            }
         }

         return null;
      }
   }

   private int activeBindingCount() {
      return this.activeConnectorGeometryList().size();
   }

   private ConnectorGeometry debugGeometryAtMouse(int mouseX, int mouseY) {
      if (this.isPointOverAnyOpenPanel(mouseX, mouseY)) {
         return null;
      } else {
         int scaledMouseX = OverlayRenderHelper.scaleX(mouseX, this.virtualWidth() / 2, this.pixelScale());
         int scaledMouseY = OverlayRenderHelper.scaleY(mouseY, this.virtualHeight() / 2, this.pixelScale());

         for (ConnectorGeometry geometry : this.activeConnectorGeometryList()) {
            if (this.hasVisibleAssignments(geometry.button.glfwKey) && geometry.button.isHovered(scaledMouseX, scaledMouseY, this.pixelScale())) {
               return geometry;
            }
         }

         return null;
      }
   }

   private void renderHiddenKeysInfoPanel(GuiGraphicsExtractor guiGraphics) {
      if (this.hiddenKeysModeActive && this.categoryInfoVisible && !this.isDebugMode && !this.editModeState.isActive() && this.font != null) {
         String message = AtlasText.text("ui.categories.assign_help");
         int msgW = Math.round((float)this.font.width(message) * 0.75F);
         int msgX = (this.virtualWidth() - msgW) / 2;
         int msgY = 26;
         int padX = 6;
         int padY = 4;
         int boxX = msgX - padX;
         int boxY = msgY - padY;
         int boxW = msgW + padX * 2;
         int boxH = Math.max(1, Math.round(9.0F * 0.75F)) + padY * 2;
         int cr = 1;
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)(0.0F), (float)(0.0F));
         OverlayRenderHelper.blitBlurredBackground(guiGraphics, boxX, boxY, boxW, boxH, boxX, boxY, boxW, boxH, 1.0F);
         OverlayRenderHelper.fillRoundedRect(
            guiGraphics,
            boxX,
            boxY,
            boxW,
            boxH,
            cr,
            OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), 0.12F), KeyVisualStyle.panelAlpha())
         );
         OverlayRenderHelper.drawRoundedRectOutline(guiGraphics, boxX, boxY, boxW, boxH, cr, KeyVisualStyle.boxBorder(), 1);
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)((float)msgX), (float)((float)msgY));
         guiGraphics.pose().scale((float)(0.75F), (float)(0.75F));
         guiGraphics.text(this.font, message, 0, 0, KeyVisualStyle.boxText(), false);
         guiGraphics.pose().popMatrix();
         guiGraphics.pose().popMatrix();
      }
   }

   private boolean isLayerModifierKey(int glfwKey) {
      return switch (this.currentLayer) {
         case F3 -> glfwKey == 292;
         case CONTROL -> glfwKey == 341 || glfwKey == 345;
         case SHIFT -> glfwKey == 340 || glfwKey == 344;
         case ALT -> glfwKey == 342 || glfwKey == 346;
         case NONE -> false;
      };
   }

   private KeyBindingModifier detectHeldModifier() {
      if (this.minecraft == null || this.minecraft.getWindow() == null) {
         return KeyBindingModifier.NONE;
      }
      var window = this.minecraft.getWindow();
      if (InputConstants.isKeyDown(window, InputConstants.KEY_F3)) {
         return KeyBindingModifier.F3;
      }
      if (InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL)) {
         return KeyBindingModifier.CONTROL;
      }
      if (InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT)) {
         return KeyBindingModifier.SHIFT;
      }
      if (InputConstants.isKeyDown(window, InputConstants.KEY_LALT) || InputConstants.isKeyDown(window, InputConstants.KEY_RALT)) {
         return KeyBindingModifier.ALT;
      }
      return KeyBindingModifier.NONE;
   }

   private void updateActiveLayer() {
      if (this.isAnyDialogOpen()) {
         return;
      }
      KeyBindingModifier held = this.detectHeldModifier();
      KeyBindingModifier target = held != KeyBindingModifier.NONE ? held : this.lockedLayer;
      if (target != this.currentLayer) {
         this.setLayer(target);
      }
   }

   private boolean isAnyDialogOpen() {
      return (this.editDialog != null && this.editDialog.isVisible())
         || (this.categoryNameDialog != null && this.categoryNameDialog.isVisible())
         || (this.confirmDialog != null && this.confirmDialog.isVisible());
   }

   private void setLayer(KeyBindingModifier target) {
      if (this.currentLayer != target) {
         this.currentLayer = target;
         this.bindingCache.setCurrentLayer(target);
         this.markBindingLayoutDirty();
      }
   }

   private int layersPanelHeight() {
      return LAYERS_PANEL_PAD * 2 + LAYERS_HEADER_H + 3 + DISPLAYED_LAYERS.length * LAYERS_ROW_H + (DISPLAYED_LAYERS.length - 1) * LAYERS_ROW_GAP;
   }

   private boolean isPointOverLayersPanel(int mouseX, int mouseY) {
      if (this.isDebugMode) {
         return false;
      }
      int x = LAYERS_PANEL_X;
      int y = LAYERS_PANEL_Y;
      int w = LAYERS_PANEL_W;
      int h = this.layersPanelHeight();
      return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
   }

   private KeyBindingModifier hitTestLayers(int mouseX, int mouseY) {
      if (!this.isPointOverLayersPanel(mouseX, mouseY)) {
         return null;
      }
      int sepY = LAYERS_PANEL_Y + LAYERS_PANEL_PAD + LAYERS_HEADER_H;
      int startY = sepY + 3;
      int rowW = LAYERS_PANEL_W - LAYERS_PANEL_PAD * 2;
      for (int i = 0; i < DISPLAYED_LAYERS.length; i++) {
         int rowY = startY + i * (LAYERS_ROW_H + LAYERS_ROW_GAP);
         if (mouseX >= LAYERS_PANEL_X + LAYERS_PANEL_PAD && mouseX < LAYERS_PANEL_X + LAYERS_PANEL_PAD + rowW
               && mouseY >= rowY && mouseY < rowY + LAYERS_ROW_H) {
            return DISPLAYED_LAYERS[i];
         }
      }
      return null;
   }

   private void renderLayersPanel(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
      if (this.isDebugMode || this.font == null) {
         return;
      }
      int x = LAYERS_PANEL_X;
      int y = LAYERS_PANEL_Y;
      int w = LAYERS_PANEL_W;
      int h = this.layersPanelHeight();
      int cr = KeyVisualStyle.PANEL_CORNER;

      OverlayRenderHelper.blitBlurredBackground(guiGraphics, x, y, w, h, x, y, w, h, 1.0F);
      OverlayRenderHelper.fillRoundedRect(
         guiGraphics,
         x,
         y,
         w,
         h,
         cr,
         OverlayRenderHelper.withAlpha(OverlayRenderHelper.darken(KeyVisualStyle.boxFill(), KeyVisualStyle.PANEL_DARKEN), KeyVisualStyle.panelAlpha())
      );
      OverlayRenderHelper.drawRoundedRectOutline(guiGraphics, x, y, w, h, cr, KeyVisualStyle.boxBorder(), 1);

      String headerText = AtlasText.text("ui.layer.title");
      guiGraphics.pose().pushMatrix();
      guiGraphics.pose().translate((float)(x + LAYERS_PANEL_PAD), (float)(y + LAYERS_PANEL_PAD + 1));
      guiGraphics.pose().scale(0.7F, 0.7F);
      guiGraphics.text(this.font, headerText, 0, 0, KeyVisualStyle.boxSourceText(), false);
      guiGraphics.pose().popMatrix();

      int sepY = y + LAYERS_PANEL_PAD + LAYERS_HEADER_H;
      guiGraphics.fill(x + LAYERS_PANEL_PAD, sepY, x + w - LAYERS_PANEL_PAD, sepY + 1, OverlayRenderHelper.withAlpha(KeyVisualStyle.boxBorder(), 0.5F));

      int startY = sepY + 3;
      int rowW = w - LAYERS_PANEL_PAD * 2;
      KeyBindingModifier held = this.detectHeldModifier();

      for (int i = 0; i < DISPLAYED_LAYERS.length; i++) {
         KeyBindingModifier layer = DISPLAYED_LAYERS[i];
         int rowY = startY + i * (LAYERS_ROW_H + LAYERS_ROW_GAP);
         boolean isCurrent = (this.currentLayer == layer);
         boolean isHeld = (held == layer);
         boolean hovered = mouseX >= x + LAYERS_PANEL_PAD && mouseX < x + LAYERS_PANEL_PAD + rowW
            && mouseY >= rowY && mouseY < rowY + LAYERS_ROW_H;

         if (isCurrent) {
            int fill = isHeld ? KeyVisualStyle.boxHoverFill() : OverlayRenderHelper.darken(KeyVisualStyle.boxHoverFill(), 0.15F);
            OverlayRenderHelper.fillRoundedRect(guiGraphics, x + LAYERS_PANEL_PAD, rowY, rowW, LAYERS_ROW_H, 2, fill);
            int border = isHeld ? KeyVisualStyle.keyAssignedFill() : KeyVisualStyle.boxHoverBorder();
            OverlayRenderHelper.drawRoundedRectOutline(guiGraphics, x + LAYERS_PANEL_PAD, rowY, rowW, LAYERS_ROW_H, 2, border, 1);
         } else if (hovered) {
            OverlayRenderHelper.fillRoundedRect(guiGraphics, x + LAYERS_PANEL_PAD, rowY, rowW, LAYERS_ROW_H, 2,
               OverlayRenderHelper.withAlpha(KeyVisualStyle.boxHoverFill(), 0.4F));
            OverlayRenderHelper.drawRoundedRectOutline(guiGraphics, x + LAYERS_PANEL_PAD, rowY, rowW, LAYERS_ROW_H, 2,
               OverlayRenderHelper.withAlpha(KeyVisualStyle.boxHoverBorder(), 0.5F), 1);
         }

         String name = layer.layerDisplayName();
         int textColor = isCurrent ? KeyVisualStyle.keyAssignedText() : (hovered ? KeyVisualStyle.boxText() : KeyVisualStyle.keyUnusedText());
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)(x + LAYERS_PANEL_PAD + 3), (float)(rowY + 3));
         guiGraphics.pose().scale(0.8F, 0.8F);
         guiGraphics.text(this.font, name, 0, 0, textColor, false);
         guiGraphics.pose().popMatrix();

         int count = this.bindingCache.getBindingCountForLayer(layer);
         String countStr = String.valueOf(count);
         float badgeScale = 0.65F;
         int badgeTextW = Math.round(this.font.width(countStr) * badgeScale);
         int badgePadX = 2;
         int badgeW = badgeTextW + badgePadX * 2;
         int badgeH = 9;
         int badgeX = x + w - LAYERS_PANEL_PAD - badgeW - 2;
         int badgeY = rowY + (LAYERS_ROW_H - badgeH) / 2;

         int conflictCount = this.bindingCache.getConflictCountForLayer(layer);
         int badgeBg;
         int badgeTextColor;
         if (conflictCount > 0) {
            badgeBg = OverlayRenderHelper.withAlpha(KeyVisualStyle.keyDirectConflictFill(), 0.9F);
            badgeTextColor = -1;
         } else if (isCurrent) {
            badgeBg = OverlayRenderHelper.withAlpha(KeyVisualStyle.keyAssignedFill(), 0.7F);
            badgeTextColor = -1;
         } else if (count > 0) {
            badgeBg = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxFill(), 0.8F);
            badgeTextColor = KeyVisualStyle.boxText();
         } else {
            badgeBg = OverlayRenderHelper.withAlpha(KeyVisualStyle.boxFill(), 0.3F);
            badgeTextColor = KeyVisualStyle.boxSourceText();
         }

         OverlayRenderHelper.fillRoundedRect(guiGraphics, badgeX, badgeY, badgeW, badgeH, 2, badgeBg);
         guiGraphics.pose().pushMatrix();
         guiGraphics.pose().translate((float)(badgeX + badgePadX), (float)(badgeY + 1));
         guiGraphics.pose().scale(badgeScale, badgeScale);
         guiGraphics.text(this.font, countStr, 0, 0, badgeTextColor, false);
         guiGraphics.pose().popMatrix();
      }
   }

   private void jumpDebugIndex(int targetIndex) {
      int maxIndex = this.activeBindingCount() - 1;
      int clampedIndex = maxIndex < 0 ? -1 : Math.max(-1, Math.min(maxIndex, targetIndex));
      if (clampedIndex != this.debugIndex) {
         if (clampedIndex > this.debugIndex) {
            for (int index = Math.max(0, this.debugIndex + 1); index <= clampedIndex; index++) {
               KeyboardKey key = this.debugKeyAtIndex(index);
               if (key != null) {
                  this.boxAnimStart.remove(this.animationKey(key));
               }
            }
         }

         this.debugIndex = clampedIndex;
         this.markStaticLayerDirty();
      }
   }

   private KeyboardScreen.StaticBoxEntry resolveBoxPlacement(ConnectorGeometry geometry, BoxTextLayout layout) {
      int x;
      if (geometry.boxLeft) {
         x = geometry.boxPos.boxX + geometry.nudgeX + geometry.boxPos.boxWidth - layout.dimensions.width;
      } else if (geometry.boxRight) {
         x = geometry.boxPos.boxX + geometry.nudgeX;
      } else {
         x = geometry.boxPos.boxX + geometry.nudgeX + (geometry.boxPos.boxWidth - layout.dimensions.width) / 2;
      }

      int y;
      if (geometry.boxAbove) {
         y = geometry.boxPos.boxY + geometry.nudgeY + geometry.boxPos.boxHeight - layout.dimensions.height;
      } else if (geometry.boxBelow) {
         y = geometry.boxPos.boxY + geometry.nudgeY;
      } else {
         y = geometry.boxPos.boxY + geometry.nudgeY + (geometry.boxPos.boxHeight - layout.dimensions.height) / 2;
      }

      int screenCenterX = this.virtualWidth() / 2;
      int screenCenterY = this.virtualHeight() / 2;
      int scaledMinX = OverlayRenderHelper.scaleX(0, screenCenterX, this.pixelScale());
      int scaledMaxX = OverlayRenderHelper.scaleX(this.virtualWidth(), screenCenterX, this.pixelScale());
      int scaledMinY = OverlayRenderHelper.scaleY(0, screenCenterY, this.pixelScale());
      int scaledMaxY = OverlayRenderHelper.scaleY(this.virtualHeight(), screenCenterY, this.pixelScale());
      int maxX = scaledMaxX - layout.dimensions.width;
      int maxY = scaledMaxY - layout.dimensions.height;
      x = maxX < scaledMinX ? scaledMinX : Math.max(scaledMinX, Math.min(x, maxX));
      y = maxY < scaledMinY ? scaledMinY : Math.max(scaledMinY, Math.min(y, maxY));
      return new KeyboardScreen.StaticBoxEntry(x, y, layout, this.legendCategoryIdForKey(geometry.button));
   }

   public boolean isPauseScreen() {
      return false;
   }

   private boolean isPointOverAnyOpenPanel(int mouseX, int mouseY) {
      if (this.isPointOverLayersPanel(mouseX, mouseY)) {
         return true;
      } else if (this.categoryNameDialog != null && this.categoryNameDialog.isVisible()) {
         return true;
      } else if (this.confirmDialog != null && this.confirmDialog.isVisible()) {
         return true;
      } else if (this.editDialog != null && this.editDialog.isVisible()) {
         return true;
      } else if (this.cachedEditModeOverlay != null
         && this.cachedEditModeOverlay.isPointOverToolbar(mouseX, mouseY, this.editModeState.isActive(), this.hiddenKeysModeActive)) {
         return true;
      } else {
         KeyboardDebugOverlay overlay = this.cachedDebugOverlay;
         if (overlay != null
            && overlay.hitTest(
                     mouseX,
                     mouseY,
                     this.isDebugMode,
                     this.activeBindingCount() > 0,
                     this.settingsOpen,
                     this.restorePending,
                     this.settingsDebugExpanded,
                     this.layoutMenuOpen,
                     this.settingsPanelProgress,
                     this.hiddenKeysModeActive
                  )
                  .action
               != KeyboardDebugOverlay.Action.NONE) {
            return true;
         } else if (overlay != null
            && overlay.isPointOverPanel(mouseX, mouseY, this.settingsOpen, this.settingsDebugExpanded, this.restorePending, this.layoutMenuOpen)) {
            return true;
         } else if (this.hiddenKeysModeActive && this.categoryPanel != null && this.categoryPanel.isPointOverPanel(mouseX, mouseY, this.categoryPanelEntries())
            )
          {
            return true;
         } else {
            KeyboardLegendOverlay legend = this.cachedLegendOverlay;
            if (legend != null && legend.isPointOverLegendPanel(mouseX, mouseY, this.font, this.buildLegendEntries())) {
               return true;
            } else {
               return legend != null && legend.isPointOverModFilter(mouseX, mouseY, this.font, this.modFilterOpen, this.easeInOut(this.modFilterPanelProgress))
                  ? true
                  : this.editModeState.isActive()
                     && this.unassignedPanel != null
                     && this.unassignedPanel.panelProgress() > 0.0F
                     && this.unassignedPanel.isPointOverPanel(mouseX, mouseY, this.unassignedByMod);
            }
         }
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float s = this.responsiveScale();
      mouseX /= (double) s;
      mouseY /= (double) s;
      if (this.categoryNameDialog != null && this.categoryNameDialog.isVisible()) {
         CategoryNameDialog.Result dialogResult = this.categoryNameDialog
            .hitTest((int)mouseX, (int)mouseY, KeybindAtlasClientConfig.presetCategoryColors(), this.categoryDialogUsedColorOwners());
         if (dialogResult != CategoryNameDialog.Result.NONE) {
            this.handleCategoryNameDialogResult(dialogResult);
            return true;
         } else if (!this.categoryNameDialog.isPointOverDialog((int)mouseX, (int)mouseY)) {
            this.categoryNameDialog.close();
            return true;
         } else {
            return true;
         }
      } else if (this.confirmDialog != null && this.confirmDialog.isVisible()) {
         ConfirmationDialog.Result confirmResult = this.confirmDialog.hitTest((int)mouseX, (int)mouseY);
         if (confirmResult == ConfirmationDialog.Result.CONFIRM) {
            this.handleConfirmAction(this.pendingConfirmAction);
            this.confirmDialog.close();
            this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.NONE;
            this.pendingDeleteCategoryId = "";
            return true;
         } else if (confirmResult == ConfirmationDialog.Result.CANCEL) {
            this.confirmDialog.close();
            this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.NONE;
            this.pendingDeleteCategoryId = "";
            return true;
         } else if (!this.confirmDialog.isPointOverDialog((int)mouseX, (int)mouseY)) {
            this.confirmDialog.close();
            this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.NONE;
            this.pendingDeleteCategoryId = "";
            return true;
         } else {
            return true;
         }
      } else if (this.editDialog != null && this.editDialog.isVisible()) {
         if (this.editDialog.isCapturing()) {
            Key mouseKey = Type.MOUSE.getOrCreate(button);
            this.editDialog.setCapturedKey(mouseKey, KeyBindingModifier.NONE);
            return true;
         } else {
            KeyBindingEditDialog.Result dialogResult = this.editDialog.hitTest((int)mouseX, (int)mouseY);
            if (dialogResult != KeyBindingEditDialog.Result.NONE) {
               this.handleEditDialogResult(dialogResult);
               return true;
            } else if (!this.editDialog.isPointOverDialog((int)mouseX, (int)mouseY)) {
               this.editDialog.close();
               return true;
            } else {
               return true;
            }
         }
      } else {
         if (!this.isDebugMode
            && this.cachedEditModeOverlay != null
            && !this.bindingCacheRefreshInProgress
            && !this.geometryRefreshInProgress
            && !this.rendererPrewarmInProgress) {
            EditModeOverlay.Action editAction = this.cachedEditModeOverlay
               .hitTest((int)mouseX, (int)mouseY, this.editModeState.isActive(), this.editModeState.canUndo(), this.hiddenKeysModeActive);
            if (editAction != EditModeOverlay.Action.NONE) {
               this.handleEditModeAction(editAction);
               return true;
            }
         }

         if (this.hiddenKeysModeActive && this.categoryPanel != null && !this.isDebugMode && !this.editModeState.isActive()) {
            KeyCategoryPanel.PanelAction panelAction = this.categoryPanel.hitTest((int)mouseX, (int)mouseY, this.categoryPanelEntries());
            if (panelAction.type != KeyCategoryPanel.PanelAction.Type.NONE) {
               this.handleCategoryPanelAction(panelAction);
               return true;
            }
         }

         if (this.editModeState.isActive() && this.unassignedPanel != null && !this.isDebugMode) {
            if (button == 0 && this.unassignedPanel.beginScrollbarDrag((int)mouseX, (int)mouseY, this.unassignedByMod)) {
               this.unassignedPanel.dragScrollbarTo((int)mouseY, this.unassignedByMod);
               return true;
            }

            UnassignedBindingsPanel.PanelAction panelAction = this.unassignedPanel.hitTest((int)mouseX, (int)mouseY, this.unassignedByMod);
            if (panelAction.type != UnassignedBindingsPanel.PanelAction.Type.NONE) {
               this.handleUnassignedPanelAction(panelAction);
               return true;
            }
         }

         if (this.editModeState.isActive() && this.hoveredBoxKey != null && this.editPenHoveredIndex >= 0) {
            String assignLabel = this.getAssignmentLabelAtPenIndex(this.hoveredBoxKey.glfwKey, this.editPenHoveredIndex);
            if (assignLabel != null && this.minecraft != null) {
               KeyMapping km = this.editModeState.findKeyMapping(this.minecraft.options.keyMappings, assignLabel, this.hoveredBoxKey.glfwKey);
               if (km != null) {
                  String currentKeyName = KeyBindingAccess.getKey(km).getDisplayName().getString();
                  KeyBindingModifier modifier = KeyBindingAccess.getModifier(km);
                  if (!modifier.isNone()) {
                     currentKeyName = modifier.formatKeyName(currentKeyName);
                  }

                  this.editDialogTranslationKey = km.getName();
                  this.editDialog.openForEdit(assignLabel, currentKeyName, this.hoveredBoxKey.glfwKey);
                  return true;
               }
            }
         }

         if (button == 0) {
            KeyBindingModifier clickedLayer = this.hitTestLayers((int)mouseX, (int)mouseY);
            if (clickedLayer != null) {
               if (this.lockedLayer == clickedLayer) {
                  this.lockedLayer = KeyBindingModifier.NONE;
                  this.setLayer(KeyBindingModifier.NONE);
               } else {
                  this.lockedLayer = clickedLayer;
                  this.setLayer(clickedLayer);
               }
               return true;
            }
         }

         if (!this.isPointOverAnyOpenPanel((int)mouseX, (int)mouseY) && KeybindAtlasClientConfig.allKeysEnabled() && !this.transitionActive) {
            int[] btn = this.auxToggleButtonBounds(1);
            if (OverlayRenderHelper.isMouseOver((int)mouseX, (int)mouseY, btn[0], btn[1], btn[2], btn[3], 0)) {
               if (this.animationsEnabled()) {
                  this.transitionActive = true;
                  this.transitionStartNanos = System.nanoTime();
                  this.transitionToAux = !this.auxKeysActive;
                  if (this.transitionToAux) {
                     this.ensureAuxBindingLayoutCache();
                  }

                  this.debugIndex = -1;
               } else {
                  if (!this.auxKeysActive) {
                     this.ensureAuxBindingLayoutCache();
                     this.auxKeysActive = true;
                     this.auxActivatedNanos = System.nanoTime();
                     this.debugIndex = -1;
                  } else {
                     this.auxKeysActive = false;
                     this.auxActivatedNanos = -1L;
                     this.debugIndex = -1;
                  }

                  this.boxAnimStart.clear();
                  this.hoverProgressByKey.clear();
                  this.markStaticLayerDirty();
               }

               return true;
            }
         }

         if (this.hiddenKeysModeActive
            && !this.isDebugMode
            && !this.editModeState.isActive()
            && !this.bindingCacheRefreshInProgress
            && !this.geometryRefreshInProgress
            && !this.rendererPrewarmInProgress
            && !this.transitionActive
            && button == 0) {
            KeyboardKey clickedKey = this.hiddenBindingKeyAtMouse((int)mouseX, (int)mouseY);
            if (clickedKey != null) {
               this.applySelectedCategoryToKey(clickedKey);
               return true;
            }
         }

         KeyboardDebugOverlay overlay = this.cachedDebugOverlay;

         try {
            KeyboardDebugOverlay.Interaction interaction;
            if (this.auxKeysActive && this.auxLayout != null) {
               this.ensureAuxBindingLayoutCache();
               boolean hasBindings = !this.cachedAuxConnectorGeometry.isEmpty();
               if (hasBindings) {
                  List<KeyboardKey> prevButtons = this.allKeys;
                  Map<KeyboardKey, BoxPosition> prevBoxPos = this.cachedBoxPositions;
                  List<ConnectorGeometry> prevConn = this.cachedConnectorGeometry;
                  int[] prevBounds = this.saveState();

                  try {
                     this.applyLayout(this.auxLayout);
                     this.cachedBoxPositions = this.cachedAuxBoxPositions;
                     this.cachedConnectorGeometry = this.cachedAuxConnectorGeometry;
                     interaction = overlay.hitTest(
                        (int)mouseX,
                        (int)mouseY,
                        this.isDebugMode,
                        true,
                        this.settingsOpen,
                        this.restorePending,
                        this.settingsDebugExpanded,
                        this.layoutMenuOpen,
                        this.settingsPanelProgress,
                        this.hiddenKeysModeActive
                     );
                  } finally {
                     this.restoreState(prevButtons, prevBoxPos, prevConn, prevBounds);
                  }
               } else {
                  interaction = overlay.hitTest(
                     (int)mouseX,
                     (int)mouseY,
                     this.isDebugMode,
                     false,
                     this.settingsOpen,
                     this.restorePending,
                     this.settingsDebugExpanded,
                     this.layoutMenuOpen,
                     this.settingsPanelProgress,
                     this.hiddenKeysModeActive
                  );
               }
            } else {
               boolean hasBindings = !this.cachedConnectorGeometry.isEmpty();
               interaction = overlay.hitTest(
                  (int)mouseX,
                  (int)mouseY,
                  this.isDebugMode,
                  hasBindings,
                  this.settingsOpen,
                  this.restorePending,
                  this.settingsDebugExpanded,
                  this.layoutMenuOpen,
                  this.settingsPanelProgress,
                  this.hiddenKeysModeActive
               );
            }

            if (this.layoutMenuOpen
               && interaction.action == KeyboardDebugOverlay.Action.NONE
               && !overlay.isPointOverPanel((int)mouseX, (int)mouseY, this.settingsOpen, this.settingsDebugExpanded, this.restorePending, this.layoutMenuOpen)) {
               this.layoutMenuOpen = false;
            }

            if (interaction.action != KeyboardDebugOverlay.Action.NONE
               && interaction.action != KeyboardDebugOverlay.Action.TOGGLE_KEYBOARD_LAYOUT_MENU
               && interaction.action != KeyboardDebugOverlay.Action.SELECT_KEYBOARD_LAYOUT) {
               this.layoutMenuOpen = false;
            }

            switch (interaction.action) {
               case CLOSE:
                  this.onClose();
                  return true;
               case TOGGLE_DEBUG:
                  if (this.editModeState.isActive()) {
                     if (this.confirmDialog != null) {
                        this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.DISCARD_FOR_DEBUG;
                        this.confirmDialog.open(AtlasText.text("ui.confirm.discard_for_debug"));
                     }
                  } else {
                     this.setHiddenKeysModeActive(false);
                     this.isDebugMode = !this.isDebugMode;
                     this.resetAnimationState();
                     if (this.isDebugMode) {
                        this.dumpDebugLog();
                     }
                  }

                  return true;
               case TOGGLE_SETTINGS:
                  this.settingsOpen = !this.settingsOpen;
                  this.layoutMenuOpen = false;
                  if (!this.settingsOpen) {
                     this.restorePending = false;
                  }

                  if (this.settingsOpen) {
                     this.modFilterOpen = false;
                     if (this.unassignedPanel != null) {
                        this.unassignedPanel.setExpanded(false);
                     }
                  }

                  return true;
               case TOGGLE_CATEGORIES:
                  this.toggleCategoriesEnabled();
                  return true;
               case TOGGLE_CATEGORY_INFO:
                  this.toggleCategoryInfoVisible();
                  return true;
               case TOGGLE_DEBUG_PANEL:
                  this.settingsDebugExpanded = !this.settingsDebugExpanded;
                  return true;
               case CYCLE_CORNERS:
                  this.scaleIndex = (this.scaleIndex + 1) % 2;
                  KeybindAtlasClientConfig.setDefaultPixelScale(this.pixelScale());
                  this.resetAnimationState();
                  this.disposeRenderer();
                  this.init();
                  return true;
               case TOGGLE_KEYBOARD_LAYOUT_MENU:
                  this.layoutMenuOpen = !this.layoutMenuOpen;
                  return true;
               case SELECT_KEYBOARD_LAYOUT:
                  this.layoutMenuOpen = false;
                  this.applyKeyboardLayoutPreset(interaction.layoutPreset);
                  return true;
               case TOGGLE_ANIMATIONS:
                  KeybindAtlasClientConfig.setAnimationsEnabled(!this.animationsEnabled());
                  this.resetAnimationState();
                  return true;
               case TOGGLE_F_KEYS:
                  KeybindAtlasClientConfig.setFKeysEnabled(!this.fKeysEnabled());
                  this.markBindingLayoutDirty();
                  this.disposeRenderer();
                  this.init();
                  return true;
               case TOGGLE_ALL_KEYS:
                  KeybindAtlasClientConfig.setAllKeysEnabled(!KeybindAtlasClientConfig.allKeysEnabled());
                  this.markBindingLayoutDirty();
                  this.disposeRenderer();
                  this.init();
                  return true;
               case TOGGLE_ALTERNATING_LINE_COLORS:
                  KeybindAtlasClientConfig.setAlternatingLineColorsEnabled(!KeybindAtlasClientConfig.alternatingLineColorsEnabled());
                  return true;
               case CYCLE_PANEL_OPACITY:
                  float curOp = KeybindAtlasClientConfig.panelOpacity();
                  float nextOp = curOp >= 0.95F ? 0.0F : curOp + 0.1F;
                  KeybindAtlasClientConfig.setPanelOpacity(nextOp);
                  this.disposeRenderer();
                  return true;
               case DUMP_DEBUG_LOG:
                  this.dumpDebugLog();
                  return true;
               case OPEN_DEBUG_FOLDER:
                  this.openDebugLogFolder();
                  return true;
               case RESTORE_DEFAULTS:
                  if (this.confirmDialog != null) {
                     this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.RESTORE_DEFAULTS;
                     this.confirmDialog.open(AtlasText.text("ui.confirm.restore_defaults"));
                  }

                  return true;
               case DEBUG_PREV:
                  this.jumpDebugIndex(this.debugIndex - 1);
                  return true;
               case DEBUG_NEXT:
                  this.jumpDebugIndex(this.debugIndex + 1);
                  return true;
               case NONE:
               default:
                  if (!this.isDebugMode && !this.bindingCacheRefreshInProgress && !this.geometryRefreshInProgress && !this.rendererPrewarmInProgress) {
                     KeyboardLegendOverlay legend = this.cachedLegendOverlay;
                     if (legend != null
                        && button == 0
                        && legend.beginScrollbarDrag(
                           (int)mouseX,
                           (int)mouseY,
                           this.font,
                           this.modSources,
                           this.modExpanded,
                           this.bindingsByMod,
                           this.modExpandProgress,
                           this.modPanelScroll,
                           this.easeInOut(this.modFilterPanelProgress)
                        )) {
                        this.modPanelScroll = legend.dragScrollbarTo(
                           (int)mouseY,
                           this.font,
                           this.modSources,
                           this.modExpanded,
                           this.bindingsByMod,
                           this.modExpandProgress,
                           this.modPanelScroll,
                           this.easeInOut(this.modFilterPanelProgress)
                        );
                        return true;
                     } else {
                        KeyboardLegendOverlay.ModAction ma = legend.hitTest(
                           (int)mouseX,
                           (int)mouseY,
                           this.font,
                           this.modSources,
                           this.modExpanded,
                           this.bindingsByMod,
                           this.modFilterOpen,
                           this.modPanelScroll,
                           this.easeInOut(this.modFilterPanelProgress)
                        );
                        switch (ma.type) {
                           case TOGGLE_BUTTON:
                              this.modFilterOpen = !this.modFilterOpen;
                              if (this.modFilterOpen) {
                                 this.settingsOpen = false;
                                 this.layoutMenuOpen = false;
                                 this.restorePending = false;
                                 if (this.unassignedPanel != null) {
                                    this.unassignedPanel.setExpanded(false);
                                 }
                              }

                              return true;
                           case TOGGLE_MOD:
                              if (ma.modIndex >= 0 && ma.modIndex < this.modSources.size()) {
                                 String mod = this.modSources.get(ma.modIndex);
                                 Boolean prev = this.modEnabled.get(mod);
                                 boolean newVal = !Boolean.TRUE.equals(prev);
                                 this.modEnabled.put(mod, newVal);
                                 List<String> disabled = new ArrayList<>();

                                 for (Entry<String, Boolean> e : this.modEnabled.entrySet()) {
                                    if (!Boolean.TRUE.equals(e.getValue())) {
                                       disabled.add(e.getKey());
                                    }
                                 }

                                 KeybindAtlasClientConfig.setDisabledMods(disabled);
                                 this.markBindingLayoutDirty();
                                 return true;
                              }

                              return true;
                           case ENABLE_ALL:
                              for (String mod : this.modSources) {
                                 this.modEnabled.put(mod, Boolean.TRUE);
                              }

                              KeybindAtlasClientConfig.setDisabledMods(Collections.emptyList());
                              this.markBindingLayoutDirty();
                              return true;
                           case DISABLE_ALL:
                              for (String mod : this.modSources) {
                                 this.modEnabled.put(mod, Boolean.FALSE);
                              }

                              KeybindAtlasClientConfig.setDisabledMods(new ArrayList<>(this.modSources));
                              this.markBindingLayoutDirty();
                              return true;
                           case EXPAND_MOD:
                              if (ma.modIndex >= 0 && ma.modIndex < this.modSources.size()) {
                                 String mod = this.modSources.get(ma.modIndex);
                                 Boolean prev = this.modExpanded.get(mod);
                                 boolean wasExpanded = prev != null && prev;
                                 this.modExpanded.put(mod, !wasExpanded);
                                 if (!this.modExpandProgress.containsKey(mod)) {
                                    this.modExpandProgress.put(mod, wasExpanded ? 1.0F : 0.0F);
                                 }
                              }

                              return true;
                           case TOGGLE_BINDING:
                              if (ma.modIndex >= 0 && ma.modIndex < this.modSources.size() && ma.bindingLabel != null) {
                                 String mod = this.modSources.get(ma.modIndex);
                                 Map<String, Boolean> modBindings = this.bindingEnabled.computeIfAbsent(mod, k -> new LinkedHashMap<>());
                                 Boolean prev = modBindings.get(ma.bindingLabel);
                                 modBindings.put(ma.bindingLabel, prev != null && !prev);
                                 this.persistDisabledBindings();
                                 this.markBindingLayoutDirty();
                              }

                              return true;
                           case SELECT_ALL_BINDINGS:
                              if (ma.modIndex >= 0 && ma.modIndex < this.modSources.size()) {
                                 String mod = this.modSources.get(ma.modIndex);
                                 List<String> bindings = this.bindingsByMod.getOrDefault(mod, List.of());
                                 Map<String, Boolean> modBindings = this.bindingEnabled.computeIfAbsent(mod, k -> new LinkedHashMap<>());
                                 boolean allOn = bindings.stream().allMatch(bx -> {
                                    Boolean v = modBindings.get(bx);
                                    return v == null || v;
                                 });
                                 boolean newVal = !allOn;

                                 for (String b : bindings) {
                                    modBindings.put(b, newVal);
                                 }

                                 this.persistDisabledBindings();
                                 this.markBindingLayoutDirty();
                              }

                              return true;
                           case NONE:
                           default:
                              return false;
                        }
                     }
                  } else {
                     if (this.isDebugMode && button == 0) {
                        ConnectorGeometry clickedGeometry = this.debugGeometryAtMouse((int)mouseX, (int)mouseY);
                        if (clickedGeometry != null) {
                           this.jumpDebugIndex(clickedGeometry.bindingIndex);
                           return true;
                        }
                     }

                     return false;
                  }
            }
         } catch (Exception var23) {
            this.showStatusMessage(AtlasText.translatable("ui.status.ui_error", var23.getMessage()));
            return true;
         }
      }
   }

   public void onClose() {
      if (this.editModeState.isActive() && this.editModeState.canUndo() && this.confirmDialog != null && !this.confirmDialog.isVisible()) {
         this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.CLOSE;
         this.confirmDialog.open(AtlasText.text("ui.confirm.discard_and_close"));
      } else {
         this.forceClose();
      }
   }

   private void forceClose() {
      if (this.editModeState.isActive() && this.minecraft != null) {
         boolean hadPendingChanges = this.editModeState.canUndo();
         this.editModeState.discard(this.minecraft.options.keyMappings);
         if (hadPendingChanges) {
            this.invalidateBindingCache();
         }
      }

      this.cancelPendingPreparation();
      this.disposePanelBlurResources();
      this.detachRenderer();
      this.minecraft.setScreenAndShow(this.parentScreen);
   }

   private void cancelPendingPreparation() {
      cancelStalePreparedGeometryFutures();
      this.geometryRefreshInProgress = false;
      this.geometryRefreshStartMs = 0L;
      this.rendererPrewarmInProgress = false;
      this.rendererPrewarmCompletedSteps = 0;
      this.rendererPrewarmTotalSteps = 0;
      this.activePreparedGeometryKey = null;
      this.rendererPrewarmState = null;
   }

   @Override
   public void resize(int width, int height) {
      this.width = width;
      this.height = height;
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.disposeRenderer();
      this.init();
   }

   public void removed() {
      super.removed();
      this.cancelPendingPreparation();
      this.detachRenderer();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.categoryNameDialog != null && this.categoryNameDialog.isVisible()) {
         CategoryNameDialog.Result dialogResult = this.categoryNameDialog
            .keyPressed(keyCode, scanCode, modifiers, KeybindAtlasClientConfig.presetCategoryColors(), this.categoryDialogUsedColorOwners());
         if (dialogResult != CategoryNameDialog.Result.NONE) {
            this.handleCategoryNameDialogResult(dialogResult);
         }

         return true;
      } else if (this.confirmDialog != null && this.confirmDialog.isVisible()) {
         if (keyCode == 256) {
            this.confirmDialog.close();
            this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.NONE;
            this.pendingDeleteCategoryId = "";
            return true;
         } else {
            return true;
         }
      } else if (this.editDialog != null && this.editDialog.isCapturing()) {
         if (keyCode == 256) {
            if (this.editDialog.isForNewAssignment()) {
               this.editDialog.close();
            } else {
               this.editDialog.close();
            }

            return true;
         } else if (keyCode != 340 && keyCode != 344 && keyCode != 341 && keyCode != 345 && keyCode != 342 && keyCode != 346) {
            Key key = InputConstants.getKey(new KeyEvent(keyCode, scanCode, modifiers));
            KeyBindingModifier mod = KeyBindingAccess.fromGlfwModifiers(modifiers);
            this.editDialog.setCapturedKey(key, mod);
            return true;
         } else {
            return true;
         }
      } else if (this.editDialog != null && this.editDialog.isVisible()) {
         if (keyCode == 256) {
            this.editDialog.close();
            return true;
         } else {
            return true;
         }
      } else {
         if (this.isDebugMode) {
            if (keyCode == 263) {
               this.jumpDebugIndex(this.debugIndex - 1);
               return true;
            }

            if (keyCode == 262) {
               this.jumpDebugIndex(this.debugIndex + 1);
               return true;
            }
         }

         if (keyCode == 256) {
            if (this.lockedLayer != KeyBindingModifier.NONE) {
               this.lockedLayer = KeyBindingModifier.NONE;
               this.setLayer(KeyBindingModifier.NONE);
               return true;
            }
            if (this.shouldCloseOnEsc()) {
               this.onClose();
               return true;
            }
         }

         return false;
      }
   }

   public boolean charTyped(char codePoint, int modifiers) {
      return this.categoryNameDialog != null && this.categoryNameDialog.isVisible()
         ? this.categoryNameDialog.charTyped(codePoint, modifiers)
         : false;
   }

   private void handleEditModeAction(EditModeOverlay.Action action) {
      switch (action) {
         case TOGGLE_EDIT:
            if (this.editModeState.isActive()) {
               if (this.minecraft != null) {
                  if (this.editModeState.canUndo()) {
                     this.editModeState.discard(this.minecraft.options.keyMappings);
                     this.invalidateBindingCache();
                  } else {
                     this.editModeState.exitEditMode();
                     this.hoveredBoxKey = null;
                     this.editPenHoveredIndex = -1;
                  }

                  this.showStatusMessage(AtlasText.translatable("ui.status.edit_mode_cancelled"));
               }
            } else if (this.minecraft != null) {
               this.setHiddenKeysModeActive(false);
               this.editModeState.enterEditMode(this.minecraft.options.keyMappings);
               this.refreshUnassignedBindings();
               this.showStatusMessage(AtlasText.translatable("ui.status.edit_mode_enabled"));
            }
            break;
         case TOGGLE_HIDDEN_KEYS:
            this.toggleHiddenKeysMode();
            break;
         case TOGGLE_CATEGORIES:
            this.toggleCategoriesEnabled();
            break;
         case UNDO:
            if (this.editModeState.canUndo() && this.minecraft != null) {
               this.editModeState.undo(this.minecraft.options.keyMappings);
               this.invalidateBindingCache();
               this.refreshUnassignedBindings();
            }
            break;
         case DISCARD:
            if (this.confirmDialog != null) {
               this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.DISCARD;
               this.confirmDialog.open(AtlasText.text("ui.confirm.discard_all_changes"));
            }
            break;
         case APPLY:
            if (this.confirmDialog != null) {
               this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.APPLY;
               this.confirmDialog.open(AtlasText.text("ui.confirm.apply_all_changes"));
            }
            break;
         case RESET:
            if (this.confirmDialog != null) {
               this.pendingConfirmAction = KeyboardScreen.PendingConfirmAction.RESET_BINDINGS;
               this.confirmDialog.open(AtlasText.text("ui.confirm.reset_all_bindings"));
            }
      }
   }

   private void handleConfirmAction(KeyboardScreen.PendingConfirmAction action) {
      switch (action) {
         case APPLY:
            if (this.minecraft != null) {
               this.editModeState.confirm(this.minecraft.options);
               this.invalidateBindingCache();
               this.showStatusMessage(AtlasText.translatable("ui.status.changes_applied"));
            }
            break;
         case DISCARD:
            if (this.minecraft != null) {
               this.editModeState.discard(this.minecraft.options.keyMappings);
               this.invalidateBindingCache();
               this.refreshUnassignedBindings();
               this.showStatusMessage(AtlasText.translatable("ui.status.changes_discarded"));
            }
            break;
         case RESTORE_DEFAULTS:
            KeybindAtlasClientConfig.restoreDefaults();
            this.markBindingLayoutDirty();
            this.disposeRenderer();
            this.init();
            this.restorePending = false;
            this.settingsOpen = false;
            this.layoutMenuOpen = false;
            this.isDebugMode = false;
            this.showStatusMessage(AtlasText.translatable("ui.status.defaults_restored"));
            break;
         case CLOSE:
            this.forceClose();
            break;
         case DISCARD_FOR_DEBUG:
            if (this.minecraft != null) {
               this.editModeState.discard(this.minecraft.options.keyMappings);
               this.invalidateBindingCache();
               this.refreshUnassignedBindings();
            }

            this.setHiddenKeysModeActive(false);
            this.isDebugMode = true;
            this.resetAnimationState();
            this.dumpDebugLog();
            this.showStatusMessage(AtlasText.translatable("ui.status.debug_mode_discarded"));
            break;
         case RESET_BINDINGS:
            if (this.minecraft != null) {
               this.editModeState.resetAllToDefaults(this.minecraft.options.keyMappings);
               this.invalidateBindingCache();
               this.refreshUnassignedBindings();
               this.showStatusMessage(AtlasText.translatable("ui.status.bindings_reset"));
            }
            break;
         case DELETE_CATEGORY:
            if (!this.pendingDeleteCategoryId.isBlank() && KeybindAtlasClientConfig.deleteCustomCategory(this.pendingDeleteCategoryId)) {
               if (this.pendingDeleteCategoryId.equals(this.selectedCategoryId)) {
                  this.selectCategory("assigned");
               }

               this.showStatusMessage(AtlasText.translatable("ui.status.category_deleted"));
               this.refreshAfterCategoryMetadataChange();
            }

            this.pendingDeleteCategoryId = "";
      }
   }

   private void handleEditDialogResult(KeyBindingEditDialog.Result result) {
      if (this.minecraft != null) {
         KeyMapping[] keyMappings = this.minecraft.options.keyMappings;
         switch (result) {
            case REMOVE:
               KeyMapping km = this.findEditDialogKeyMapping(keyMappings);
               if (km != null) {
                  this.editModeState.removeBinding(km);
                  this.invalidateBindingCache();
                  this.refreshUnassignedBindings();
               }

               this.editDialog.close();
               break;
            case START_CAPTURE:
               this.editDialog.startCapture();
               break;
            case CONFIRM_UPDATE:
               Key capturedKey = this.editDialog.getCapturedKey();
               KeyBindingModifier capturedMod = this.editDialog.getCapturedModifier();
               if (capturedKey != null) {
                  KeyMapping targetKm;
                  if (this.editDialog.isForNewAssignment() && this.editDialogTranslationKey != null) {
                     targetKm = this.editModeState.findKeyMappingByName(keyMappings, this.editDialogTranslationKey);
                  } else {
                     targetKm = this.findEditDialogKeyMapping(keyMappings);
                  }
                  if (targetKm != null) {
                     this.editModeState.updateBinding(targetKm, capturedKey, capturedMod);
                  }

                  this.invalidateBindingCache();
                  this.refreshUnassignedBindings();
               }

               this.editDialog.close();
               break;
            case CANCEL:
               this.editDialog.close();
         }
      }
   }

   private KeyMapping findEditDialogKeyMapping(KeyMapping[] keyMappings) {
      if (this.editDialogTranslationKey != null) {
         return this.editModeState.findKeyMappingByName(keyMappings, this.editDialogTranslationKey);
      } else {
         String label = this.editDialog.getBindingLabel();
         int glfwKey = this.editDialog.getSourceGlfwKey();
         return this.editModeState.findKeyMapping(keyMappings, label, glfwKey);
      }
   }

   private void handleUnassignedPanelAction(UnassignedBindingsPanel.PanelAction action) {
      switch (action.type) {
         case TOGGLE:
            this.unassignedPanel.toggle();
            if (this.unassignedPanel.isExpanded()) {
               this.refreshUnassignedBindings();
               this.settingsOpen = false;
               this.layoutMenuOpen = false;
               this.restorePending = false;
               this.modFilterOpen = false;
            }
            break;
         case EXPAND_MOD:
            List<String> modNames = new ArrayList<>(this.unassignedByMod.keySet());
            this.unassignedPanel.setModExpanded(action.modIndex, modNames);
            break;
         case ASSIGN:
            if (action.translationKey != null && this.minecraft != null) {
               KeyMapping km = this.editModeState.findKeyMappingByName(this.minecraft.options.keyMappings, action.translationKey);
               if (km != null) {
                  String label = KeyboardBindingCache.resolveBindingLabel(km);
                  this.editDialogTranslationKey = km.getName();
                  this.editDialog.openForNewAssignment(label, km.getName());
               }
            }
      }
   }

   private void refreshUnassignedBindings() {
      if (this.minecraft != null) {
         this.unassignedByMod = this.editModeState.buildUnassignedBindings(this.minecraft.options.keyMappings);
      }
   }

   private static final class DynamicBoxEntry {
      final int x;
      final int y;
      final BoxTextLayout layout;
      final boolean hovered;
      final float boxAlpha;
      final float textAlpha;

      DynamicBoxEntry(int x, int y, BoxTextLayout layout, boolean hovered, float boxAlpha, float textAlpha) {
         this.x = x;
         this.y = y;
         this.layout = layout;
         this.hovered = hovered;
         this.boxAlpha = boxAlpha;
         this.textAlpha = textAlpha;
      }
   }

   private static final class FilteredLayoutKey {
      final int glfwKey;
      final boolean hovered;
      final boolean editMode;
      final int visibleBindingBoxCount;

      private FilteredLayoutKey(int glfwKey, boolean hovered, boolean editMode, int visibleBindingBoxCount) {
         this.glfwKey = glfwKey;
         this.hovered = hovered;
         this.editMode = editMode;
         this.visibleBindingBoxCount = visibleBindingBoxCount;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardScreen.FilteredLayoutKey that)
               ? false
               : this.glfwKey == that.glfwKey
                  && this.hovered == that.hovered
                  && this.editMode == that.editMode
                  && this.visibleBindingBoxCount == that.visibleBindingBoxCount;
         }
      }

      @Override
      public int hashCode() {
         int result = this.glfwKey;
         result = 31 * result + (this.hovered ? 1 : 0);
         result = 31 * result + (this.editMode ? 1 : 0);
         return 31 * result + this.visibleBindingBoxCount;
      }
   }

   private static enum PendingConfirmAction {
      NONE,
      APPLY,
      DISCARD,
      RESTORE_DEFAULTS,
      CLOSE,
      DISCARD_FOR_DEBUG,
      RESET_BINDINGS,
      DELETE_CATEGORY;
   }

   private static final class PreparedGeometry {
      final KeyboardScreen.PreparedGeometryKey key;
      final Map<KeyboardKey, BoxPosition> boxPositions;
      final List<ConnectorGeometry> connectorGeometry;
      final int visibleBindingBoxCount;

      private PreparedGeometry(
         KeyboardScreen.PreparedGeometryKey key,
         Map<KeyboardKey, BoxPosition> boxPositions,
         List<ConnectorGeometry> connectorGeometry,
         int visibleBindingBoxCount
      ) {
         this.key = key;
         this.boxPositions = Map.copyOf(boxPositions);
         this.connectorGeometry = List.copyOf(connectorGeometry);
         this.visibleBindingBoxCount = visibleBindingBoxCount;
      }

      private static KeyboardScreen.PreparedGeometry empty(KeyboardScreen.PreparedGeometryKey key, int visibleBindingBoxCount) {
         return new KeyboardScreen.PreparedGeometry(key, Map.of(), List.of(), visibleBindingBoxCount);
      }

      private KeyboardScreen.PreparedGeometrySnapshot snapshot() {
         return new KeyboardScreen.PreparedGeometrySnapshot(this.boxPositions, this.connectorGeometry, this.visibleBindingBoxCount);
      }
   }

   private static final class PreparedGeometryKey {
      final int bindingSignature;
      final int screenWidth;
      final int screenHeight;
      final int pixelScale;
      final int filterStateHash;
      final int hiddenBindingLabelsHash;
      final boolean fKeysEnabled;
      final boolean hiddenBindingsEnabled;
      final KeyboardLayouts.MainLayoutPreset keyboardLayout;

      private PreparedGeometryKey(
         int bindingSignature,
         int screenWidth,
         int screenHeight,
         int pixelScale,
         int filterStateHash,
         int hiddenBindingLabelsHash,
         boolean fKeysEnabled,
         boolean hiddenBindingsEnabled,
         KeyboardLayouts.MainLayoutPreset keyboardLayout
      ) {
         this.bindingSignature = bindingSignature;
         this.screenWidth = screenWidth;
         this.screenHeight = screenHeight;
         this.pixelScale = pixelScale;
         this.filterStateHash = filterStateHash;
         this.hiddenBindingLabelsHash = hiddenBindingLabelsHash;
         this.fKeysEnabled = fKeysEnabled;
         this.hiddenBindingsEnabled = hiddenBindingsEnabled;
         this.keyboardLayout = keyboardLayout;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardScreen.PreparedGeometryKey that)
               ? false
               : this.bindingSignature == that.bindingSignature
                  && this.screenWidth == that.screenWidth
                  && this.screenHeight == that.screenHeight
                  && this.pixelScale == that.pixelScale
                  && this.filterStateHash == that.filterStateHash
                  && this.hiddenBindingLabelsHash == that.hiddenBindingLabelsHash
                  && this.fKeysEnabled == that.fKeysEnabled
                  && this.hiddenBindingsEnabled == that.hiddenBindingsEnabled
                  && this.keyboardLayout == that.keyboardLayout;
         }
      }

      @Override
      public int hashCode() {
         int result = this.bindingSignature;
         result = 31 * result + this.screenWidth;
         result = 31 * result + this.screenHeight;
         result = 31 * result + this.pixelScale;
         result = 31 * result + this.filterStateHash;
         result = 31 * result + this.hiddenBindingLabelsHash;
         result = 31 * result + (this.fKeysEnabled ? 1 : 0);
         result = 31 * result + (this.hiddenBindingsEnabled ? 1 : 0);
         return 31 * result + this.keyboardLayout.hashCode();
      }
   }

   private static final class PreparedGeometryRequest {
      final KeyboardScreen.PreparedGeometryKey key;
      final int screenWidth;
      final int screenHeight;
      final int pixelScale;
      final KeyboardLayout layout;
      final List<KeyboardKey> keysWithBindings;
      final List<KeyboardKey> keysWithAnyBindings;
      final int visibleBindingBoxCount;
      final Map<Integer, BoxTextLayout> layoutByGlfw;
      final BoxTextLayout emptyLayout;
      final KeyboardScreen.PreparedGeometryReuseKey reuseKey;

      private PreparedGeometryRequest(
         KeyboardScreen.PreparedGeometryKey key,
         int screenWidth,
         int screenHeight,
         int pixelScale,
         KeyboardLayout layout,
         List<KeyboardKey> keysWithBindings,
         List<KeyboardKey> keysWithAnyBindings,
         int visibleBindingBoxCount,
         Map<Integer, BoxTextLayout> layoutByGlfw,
         BoxTextLayout emptyLayout,
         KeyboardScreen.PreparedGeometryReuseKey reuseKey
      ) {
         this.key = key;
         this.screenWidth = screenWidth;
         this.screenHeight = screenHeight;
         this.pixelScale = pixelScale;
         this.layout = layout;
         this.keysWithBindings = keysWithBindings;
         this.keysWithAnyBindings = keysWithAnyBindings;
         this.visibleBindingBoxCount = visibleBindingBoxCount;
         this.layoutByGlfw = Map.copyOf(layoutByGlfw);
         this.emptyLayout = emptyLayout;
         this.reuseKey = reuseKey;
      }
   }

   private static final class PreparedGeometryReuseKey {
      final int screenWidth;
      final int screenHeight;
      final int pixelScale;
      final int keyboardMinX;
      final int keyboardMaxX;
      final int keyboardMinY;
      final int keyboardMaxY;
      final int[] mouseDeviceBounds;
      final int[] allKeyStates;
      final int[] visibleKeyStates;
      final int[] anyBindingKeys;

      private PreparedGeometryReuseKey(
         int screenWidth,
         int screenHeight,
         int pixelScale,
         int keyboardMinX,
         int keyboardMaxX,
         int keyboardMinY,
         int keyboardMaxY,
         int[] mouseDeviceBounds,
         int[] allKeyStates,
         int[] visibleKeyStates,
         int[] anyBindingKeys
      ) {
         this.screenWidth = screenWidth;
         this.screenHeight = screenHeight;
         this.pixelScale = pixelScale;
         this.keyboardMinX = keyboardMinX;
         this.keyboardMaxX = keyboardMaxX;
         this.keyboardMinY = keyboardMinY;
         this.keyboardMaxY = keyboardMaxY;
         this.mouseDeviceBounds = mouseDeviceBounds;
         this.allKeyStates = allKeyStates;
         this.visibleKeyStates = visibleKeyStates;
         this.anyBindingKeys = anyBindingKeys;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardScreen.PreparedGeometryReuseKey that)
               ? false
               : this.screenWidth == that.screenWidth
                  && this.screenHeight == that.screenHeight
                  && this.pixelScale == that.pixelScale
                  && this.keyboardMinX == that.keyboardMinX
                  && this.keyboardMaxX == that.keyboardMaxX
                  && this.keyboardMinY == that.keyboardMinY
                  && this.keyboardMaxY == that.keyboardMaxY
                  && Arrays.equals(this.mouseDeviceBounds, that.mouseDeviceBounds)
                  && Arrays.equals(this.allKeyStates, that.allKeyStates)
                  && Arrays.equals(this.visibleKeyStates, that.visibleKeyStates)
                  && Arrays.equals(this.anyBindingKeys, that.anyBindingKeys);
         }
      }

      @Override
      public int hashCode() {
         int result = this.screenWidth;
         result = 31 * result + this.screenHeight;
         result = 31 * result + this.pixelScale;
         result = 31 * result + this.keyboardMinX;
         result = 31 * result + this.keyboardMaxX;
         result = 31 * result + this.keyboardMinY;
         result = 31 * result + this.keyboardMaxY;
         result = 31 * result + Arrays.hashCode(this.mouseDeviceBounds);
         result = 31 * result + Arrays.hashCode(this.allKeyStates);
         result = 31 * result + Arrays.hashCode(this.visibleKeyStates);
         return 31 * result + Arrays.hashCode(this.anyBindingKeys);
      }
   }

   private static final class PreparedGeometrySnapshot {
      final Map<KeyboardKey, BoxPosition> boxPositions;
      final List<ConnectorGeometry> connectorGeometry;
      final int visibleBindingBoxCount;

      private PreparedGeometrySnapshot(Map<KeyboardKey, BoxPosition> boxPositions, List<ConnectorGeometry> connectorGeometry, int visibleBindingBoxCount) {
         this.boxPositions = Map.copyOf(boxPositions);
         this.connectorGeometry = List.copyOf(connectorGeometry);
         this.visibleBindingBoxCount = visibleBindingBoxCount;
      }

      private KeyboardScreen.PreparedGeometry toPreparedGeometry(KeyboardScreen.PreparedGeometryKey key) {
         return new KeyboardScreen.PreparedGeometry(key, this.boxPositions, this.connectorGeometry, this.visibleBindingBoxCount);
      }
   }

   private static final class RendererKeySpriteTask {
      final int width;
      final int height;
      final KeyboardRenderer.KeyRenderStyle style;

      private RendererKeySpriteTask(int width, int height, KeyboardRenderer.KeyRenderStyle style) {
         this.width = width;
         this.height = height;
         this.style = style;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardScreen.RendererKeySpriteTask that)
               ? false
               : this.width == that.width && this.height == that.height && this.style.equals(that.style);
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         result = 31 * result + this.height;
         return 31 * result + this.style.hashCode();
      }
   }

   private static final class RendererLabelTask {
      final String label;
      final int width;
      final int height;

      private RendererLabelTask(String label, int width, int height) {
         this.label = label;
         this.width = width;
         this.height = height;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardScreen.RendererLabelTask that)
               ? false
               : this.width == that.width && this.height == that.height && this.label.equals(that.label);
         }
      }

      @Override
      public int hashCode() {
         int result = this.label.hashCode();
         result = 31 * result + this.width;
         return 31 * result + this.height;
      }
   }

   private static final class RendererPanelSpriteTask {
      final int width;
      final int height;
      final boolean hovered;

      private RendererPanelSpriteTask(int width, int height, boolean hovered) {
         this.width = width;
         this.height = height;
         this.hovered = hovered;
      }

      @Override
      public boolean equals(Object other) {
         if (this == other) {
            return true;
         } else {
            return !(other instanceof KeyboardScreen.RendererPanelSpriteTask that)
               ? false
               : this.width == that.width && this.height == that.height && this.hovered == that.hovered;
         }
      }

      @Override
      public int hashCode() {
         int result = this.width;
         result = 31 * result + this.height;
         return 31 * result + (this.hovered ? 1 : 0);
      }
   }

   private static final class RendererPrewarmState {
      final KeyboardScreen.PreparedGeometryKey key;
      final int housingWidth;
      final int housingHeight;
      final int[] mouseBounds;
      final List<KeyboardKey> mouseKeys;
      final Map<Integer, KeyboardRenderer.KeyRenderStyle> keyStyles;
      final List<KeyboardScreen.RendererKeySpriteTask> keyTasks;
      final List<KeyboardScreen.RendererPanelSpriteTask> panelTasks;
      final List<KeyboardScreen.RendererLabelTask> labelTasks;
      boolean housingDone = false;
      boolean mouseDone = false;
      int keyIndex = 0;
      int panelIndex = 0;
      int labelIndex = 0;

      private RendererPrewarmState(
         KeyboardScreen.PreparedGeometryKey key,
         int housingWidth,
         int housingHeight,
         int[] mouseBounds,
         List<KeyboardKey> mouseKeys,
         Map<Integer, KeyboardRenderer.KeyRenderStyle> keyStyles,
         List<KeyboardScreen.RendererKeySpriteTask> keyTasks,
         List<KeyboardScreen.RendererPanelSpriteTask> panelTasks,
         List<KeyboardScreen.RendererLabelTask> labelTasks
      ) {
         this.key = key;
         this.housingWidth = housingWidth;
         this.housingHeight = housingHeight;
         this.mouseBounds = mouseBounds;
         this.mouseKeys = mouseKeys;
         this.keyStyles = keyStyles;
         this.keyTasks = keyTasks;
         this.panelTasks = panelTasks;
         this.labelTasks = labelTasks;
      }

      private void prewarmNext(KeyboardRenderer renderer, int budget) {
         int remaining = Math.max(1, budget);

         while (remaining > 0 && !this.isComplete()) {
            if (!this.housingDone) {
               renderer.prewarmHousingSprite(this.housingWidth, this.housingHeight);
               this.housingDone = true;
               remaining--;
            } else if (!this.mouseDone && this.mouseBounds != null && !this.mouseKeys.isEmpty()) {
               renderer.prewarmMouseDevice(
                  this.mouseBounds[2],
                  this.mouseBounds[3],
                  this.mouseKeys,
                  key -> this.keyStyles.getOrDefault(key.glfwKey, KeyboardRenderer.KeyRenderStyle.standard(KeyboardRenderer.KeySpriteVariant.UNUSED))
               );
               this.mouseDone = true;
               remaining--;
            } else if (this.keyIndex < this.keyTasks.size()) {
               KeyboardScreen.RendererKeySpriteTask task = this.keyTasks.get(this.keyIndex++);
               renderer.prewarmKeySprite(task.width, task.height, task.style);
               remaining--;
            } else if (this.panelIndex < this.panelTasks.size()) {
               KeyboardScreen.RendererPanelSpriteTask task = this.panelTasks.get(this.panelIndex++);
               renderer.prewarmPanelSprite(task.width, task.height, task.hovered);
               remaining--;
            } else if (this.labelIndex < this.labelTasks.size()) {
               KeyboardScreen.RendererLabelTask task = this.labelTasks.get(this.labelIndex++);
               renderer.prewarmCenteredLabel(task.label, task.width, task.height);
               remaining--;
            }
         }
      }

      private int totalSteps() {
         int total = this.keyTasks.size() + this.panelTasks.size() + this.labelTasks.size();
         if (this.housingWidth > 0 && this.housingHeight > 0) {
            total++;
         }

         if (this.mouseBounds != null && !this.mouseKeys.isEmpty()) {
            total++;
         }

         return Math.max(1, total);
      }

      private int completedSteps() {
         int total = this.keyIndex + this.panelIndex + this.labelIndex;
         if (this.housingDone && this.housingWidth > 0 && this.housingHeight > 0) {
            total++;
         }

         if (this.mouseDone && this.mouseBounds != null && !this.mouseKeys.isEmpty()) {
            total++;
         }

         return total;
      }

      private boolean isComplete() {
         boolean housingComplete = this.housingWidth <= 0 || this.housingHeight <= 0 || this.housingDone;
         boolean mouseComplete = this.mouseBounds == null || this.mouseKeys.isEmpty() || this.mouseDone;
         return housingComplete
            && mouseComplete
            && this.keyIndex >= this.keyTasks.size()
            && this.panelIndex >= this.panelTasks.size()
            && this.labelIndex >= this.labelTasks.size();
      }
   }

   private static final class SequentialGeometryResult {
      final Map<KeyboardKey, BoxPosition> boxPositions;
      final List<ConnectorGeometry> connectorGeometry;

      private SequentialGeometryResult(Map<KeyboardKey, BoxPosition> boxPositions, List<ConnectorGeometry> connectorGeometry) {
         this.boxPositions = new LinkedHashMap<>(boxPositions);
         this.connectorGeometry = List.copyOf(connectorGeometry);
      }

      private static KeyboardScreen.SequentialGeometryResult empty() {
         return new KeyboardScreen.SequentialGeometryResult(Map.of(), List.of());
      }
   }

   private static final class StaticBoxEntry {
      final int x;
      final int y;
      final BoxTextLayout layout;
      final String legendCategoryId;

      StaticBoxEntry(int x, int y, BoxTextLayout layout, String legendCategoryId) {
         this.x = x;
         this.y = y;
         this.layout = layout;
         this.legendCategoryId = legendCategoryId;
      }
   }

   private static final class StaticKeyEntry {
      final KeyboardKey button;
      final boolean hasBinding;
      final KeyboardRenderer.KeyRenderStyle style;

      StaticKeyEntry(KeyboardKey button, boolean hasBinding, KeyboardRenderer.KeyRenderStyle style) {
         this.button = button;
         this.hasBinding = hasBinding;
         this.style = style;
      }
   }

   private static final class StaticLayerPlan {
      final int housingX;
      final int housingY;
      final int housingWidth;
      final int housingHeight;
      final int[] mouseDeviceBounds;
      final List<KeyboardScreen.StaticKeyEntry> keys;
      final List<KeyboardScreen.StaticBoxEntry> boxes;

      StaticLayerPlan(
         int housingX,
         int housingY,
         int housingWidth,
         int housingHeight,
         int[] mouseDeviceBounds,
         List<KeyboardScreen.StaticKeyEntry> keys,
         List<KeyboardScreen.StaticBoxEntry> boxes
      ) {
         this.housingX = housingX;
         this.housingY = housingY;
         this.housingWidth = housingWidth;
         this.housingHeight = housingHeight;
         this.mouseDeviceBounds = mouseDeviceBounds;
         this.keys = List.copyOf(keys);
         this.boxes = List.copyOf(boxes);
      }

      static KeyboardScreen.StaticLayerPlan empty() {
         return new KeyboardScreen.StaticLayerPlan(0, 0, 0, 0, null, List.of(), List.of());
      }

      boolean isEmpty() {
         return this.housingWidth <= 0 || this.housingHeight <= 0;
      }
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.render(guiGraphics, mouseX, mouseY, partialTick);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
      return this.mouseClicked(event.x(), event.y(), event.button());
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      return this.mouseReleased(event.x(), event.y(), event.button());
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
      return this.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      return this.keyPressed(event.key(), event.scancode(), event.modifiers());
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      return this.charTyped((char)event.codepoint(), 0);
   }
}
