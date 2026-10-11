package com.yourstudio.aerolauncher;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.yourstudio.aerolauncher.badges.BadgeStore;
import com.yourstudio.aerolauncher.focus.FocusPreferences;
import com.yourstudio.aerolauncher.launcher.AppActions;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppRepository;
import com.yourstudio.aerolauncher.launcher.DefaultLauncherHelper;
import com.yourstudio.aerolauncher.layout.HomeItem;
import com.yourstudio.aerolauncher.layout.HomeLayout;
import com.yourstudio.aerolauncher.layout.ItemLocation;
import com.yourstudio.aerolauncher.layout.LayoutRepository;
import com.yourstudio.aerolauncher.privacy.AppLockHelper;
import com.yourstudio.aerolauncher.privacy.AppLockPreferences;
import com.yourstudio.aerolauncher.search.SearchController;
import com.yourstudio.aerolauncher.ui.AppGridAdapter;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;
import com.yourstudio.aerolauncher.ui.FolderPopup;
import com.yourstudio.aerolauncher.ui.HomeDragListener;
import com.yourstudio.aerolauncher.ui.HomeGestureRouter;
import com.yourstudio.aerolauncher.ui.HomePagerAdapter;
import com.yourstudio.aerolauncher.ui.PageDotsController;
import com.yourstudio.aerolauncher.ui.QuickPanelController;
import com.yourstudio.aerolauncher.ui.SearchResultsAdapter;
import com.yourstudio.aerolauncher.ui.WiggleAnimation;
import com.yourstudio.aerolauncher.wallpaper.WallpaperController;
import com.yourstudio.aerolauncher.widgets.WidgetHostManager;
import com.yourstudio.aerolauncher.widgets.WidgetInstaller;
import com.yourstudio.aerolauncher.widgets.WidgetPlacement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Home launcher: workspace, widgets, App Library, search, quick panel, and gestures.
 */
public class MainActivity extends AppCompatActivity
        implements AppRepository.Listener,
        LayoutRepository.Listener,
        AppGridAdapter.Callbacks,
        HomeGestureRouter.Callbacks,
        HomePagerAdapter.WidgetCallbacks,
        BadgeStore.Listener {

    private AppRepository appRepository;
    private LayoutRepository layoutRepository;
    private WidgetHostManager widgetHostManager;
    private WidgetInstaller widgetInstaller;
    private HomePagerAdapter pagerAdapter;
    private PageDotsController pageDots;
    private ViewPager2 homePager;
    private LinearLayout dockRow;
    private MaterialCardView defaultBanner;
    private MaterialButton editDone;
    private View searchOverlay;
    private EditText searchInput;
    private SearchResultsAdapter searchAdapter;
    private HomeGestureRouter gestureRouter;
    private QuickPanelController quickPanel;
    private WallpaperController wallpaperController;
    private final Map<String, AppInfo> appsByKey = new HashMap<>();
    private boolean keepSplash = true;
    private boolean editMode;
    private boolean searchOpen;
    private final Handler pageFlipHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingPageFlip;

    private int pendingWidgetPage = 0;
    @Nullable
    private WidgetPlacement pendingStackTarget;

    private final ActivityResultLauncher<Intent> homeRoleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    updateDefaultLauncherBanner());

    private final ActivityResultLauncher<Intent> bindWidgetLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (widgetInstaller != null) {
                    widgetInstaller.onBindResult(result.getResultCode(), result.getData());
                }
            });

    private final ActivityResultLauncher<Intent> configureWidgetLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (widgetInstaller != null) {
                    widgetInstaller.onConfigureResult(result.getResultCode(), result.getData());
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (quickPanel != null) {
                    quickPanel.onCameraPermissionResult(Boolean.TRUE.equals(granted));
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> keepSplash);
        ThemePreferences.applyAccentOverlay(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EdgeToEdge.apply(this, findViewById(R.id.root));

        AeroLauncherApp app = (AeroLauncherApp) getApplication();
        appRepository = app.getAppRepository();
        layoutRepository = app.getLayoutRepository();
        widgetHostManager = app.getWidgetHostManager();
        widgetHostManager.startListening();
        widgetInstaller = new WidgetInstaller(
                widgetHostManager, bindWidgetLauncher, configureWidgetLauncher);
        wallpaperController = new WallpaperController(this, findViewById(R.id.home_wallpaper));

        homePager = findViewById(R.id.home_pager);
        dockRow = findViewById(R.id.dock_row);
        defaultBanner = findViewById(R.id.default_launcher_banner);
        editDone = findViewById(R.id.edit_done);
        pageDots = new PageDotsController(findViewById(R.id.page_dots));
        pagerAdapter = new HomePagerAdapter(appRepository.getIconCache(), appsByKey);
        pagerAdapter.setCallbacks(this);
        pagerAdapter.setWidgetCallbacks(this);
        pagerAdapter.setWidgetHostManager(widgetHostManager);
        homePager.setAdapter(pagerAdapter);
        homePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pageDots.setSelected(position);
            }
        });
        homePager.setOnDragListener((v, event) -> {
            if (!editMode) {
                return false;
            }
            if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
                maybeFlipPageForDrag(event.getX(), v.getWidth());
            } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED
                    || event.getAction() == DragEvent.ACTION_DROP) {
                cancelPageFlip();
            }
            return true;
        });

        setupSearchUi();
        quickPanel = new QuickPanelController(
                this,
                findViewById(R.id.quick_panel_overlay),
                () -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        gestureRouter = new HomeGestureRouter(findViewById(R.id.root), this);

        findViewById(R.id.set_default_launcher).setOnClickListener(v ->
                DefaultLauncherHelper.requestDefaultLauncher(this, homeRoleLauncher));
        findViewById(R.id.open_settings).setOnClickListener(v -> openLauncherSettings());
        editDone.setOnClickListener(v -> setEditMode(false));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (quickPanel != null && quickPanel.isOpen()) {
                    quickPanel.close();
                    return;
                }
                if (searchOpen) {
                    closeSearch();
                    return;
                }
                if (editMode) {
                    setEditMode(false);
                    return;
                }
                if (homePager != null && homePager.getCurrentItem() != 0) {
                    homePager.setCurrentItem(0, true);
                }
            }
        });

        ensureDockSlots();
        appRepository.addListener(this);
        layoutRepository.addListener(this);
        appRepository.start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (gestureRouter != null) {
            gestureRouter.observe(ev);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void setupSearchUi() {
        searchOverlay = findViewById(R.id.search_overlay);
        searchInput = searchOverlay.findViewById(R.id.search_input);
        RecyclerView results = searchOverlay.findViewById(R.id.search_results);
        searchAdapter = new SearchResultsAdapter(appRepository.getIconCache(), this::closeSearch);
        results.setLayoutManager(new LinearLayoutManager(this));
        results.setAdapter(searchAdapter);
        searchOverlay.findViewById(R.id.search_close).setOnClickListener(v -> closeSearch());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                runSearch(s != null ? s.toString() : "");
            }
        });
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchInput.getText() != null ? searchInput.getText().toString() : "";
            if (!query.trim().isEmpty()) {
                SearchController.openWebSearch(this, query);
                closeSearch();
            }
            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        widgetHostManager.startListening();
        BadgeStore.get().addListener(this);
    }

    @Override
    protected void onStop() {
        BadgeStore.get().removeListener(this);
        widgetHostManager.stopListening();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDefaultLauncherBanner();
        if (wallpaperController != null) {
            wallpaperController.refresh();
        }
        HomeLayout current = layoutRepository.getLayout();
        int capacity = HomePreferences.pageCapacity(this);
        if (!appsByKey.isEmpty()
                && !current.pages.isEmpty()
                && current.pages.get(0).size() != capacity) {
            layoutRepository.loadAndSync(appRepository.getApps());
        } else {
            bindLayout(current);
        }
        refreshLibraryData();
    }

    @Override
    protected void onDestroy() {
        appRepository.removeListener(this);
        layoutRepository.removeListener(this);
        if (widgetInstaller != null) {
            widgetInstaller.cancelPending();
        }
        if (wallpaperController != null) {
            wallpaperController.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onBadgesChanged() {
        if (pagerAdapter != null) {
            pagerAdapter.refreshBadges();
        }
        bindDock(layoutRepository.getLayout());
    }

    @Override
    public void onAppsUpdated(@NonNull List<AppInfo> apps) {
        appsByKey.clear();
        for (AppInfo appInfo : apps) {
            appsByKey.put(appInfo.key, appInfo);
        }
        layoutRepository.loadAndSync(apps);
        refreshLibraryData();
    }

    @Override
    public void onLayoutChanged(@NonNull HomeLayout layout) {
        keepSplash = false;
        bindLayout(layout);
        updateDefaultLauncherBanner();
        refreshLibraryData();
    }

    private void refreshLibraryData() {
        if (pagerAdapter != null) {
            pagerAdapter.setLibraryData(
                    appRepository.getApps(),
                    layoutRepository.getLayout().hiddenAppKeys);
        }
    }

    private void bindLayout(@NonNull HomeLayout layout) {
        int columns = HomePreferences.getColumns(this);
        int previousPage = homePager.getCurrentItem();
        List<List<HomeItem>> visiblePages = applyFocusFilter(layout.pages);
        pagerAdapter.setPages(visiblePages, layout.pageWidgets, columns);
        int pageCount = pagerAdapter.getPageCount();
        pageDots.setPageCount(pageCount);
        if (previousPage >= pageCount) {
            homePager.setCurrentItem(pageCount - 1, false);
        }
        pageDots.setSelected(homePager.getCurrentItem());
        bindDock(layout);
        refreshLibraryData();
    }

    @NonNull
    private List<List<HomeItem>> applyFocusFilter(@NonNull List<List<HomeItem>> pages) {
        Set<String> hidden = FocusPreferences.activeHiddenKeys(this);
        if (hidden.isEmpty()) {
            return pages;
        }
        List<List<HomeItem>> filtered = new ArrayList<>();
        for (List<HomeItem> page : pages) {
            List<HomeItem> copy = new ArrayList<>(page.size());
            for (HomeItem item : page) {
                if (item != null && item.isApp() && item.appKey != null && hidden.contains(item.appKey)) {
                    copy.add(null);
                } else if (item != null && item.isFolder()) {
                    copy.add(filterFolderForFocus(item, hidden));
                } else {
                    copy.add(item);
                }
            }
            filtered.add(copy);
        }
        return filtered;
    }

    @Nullable
    private HomeItem filterFolderForFocus(@NonNull HomeItem folder, @NonNull Set<String> hidden) {
        List<String> keys = new ArrayList<>();
        for (String key : folder.folderAppKeys) {
            if (!hidden.contains(key)) {
                keys.add(key);
            }
        }
        if (keys.isEmpty()) {
            return null;
        }
        if (keys.size() == folder.folderAppKeys.size()) {
            return folder;
        }
        return HomeItem.folder(folder.id, folder.title, keys);
    }

    private void ensureDockSlots() {
        dockRow.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < HomePreferences.DOCK_SLOTS; i++) {
            View slot = inflater.inflate(R.layout.item_dock_slot, dockRow, false);
            dockRow.addView(slot);
        }
    }

    private void bindDock(@NonNull HomeLayout layout) {
        for (int i = 0; i < HomePreferences.DOCK_SLOTS; i++) {
            View slot = dockRow.getChildAt(i);
            if (slot == null) {
                continue;
            }
            ImageView icon = slot.findViewById(R.id.dock_icon);
            ImageButton remove = slot.findViewById(R.id.dock_remove);
            ItemLocation location = ItemLocation.dock(i);
            HomeItem item = layout.getDock(i);
            Set<String> focusHidden = FocusPreferences.activeHiddenKeys(this);
            if (item != null && item.isApp() && item.appKey != null && focusHidden.contains(item.appKey)) {
                item = null;
            }

            slot.setOnDragListener(new HomeDragListener(location, this));
            remove.setVisibility(editMode && item != null ? View.VISIBLE : View.GONE);

            if (item == null) {
                icon.setImageDrawable(null);
                WiggleAnimation.stop(icon);
                slot.setOnClickListener(null);
                slot.setOnLongClickListener(null);
                remove.setOnClickListener(null);
                continue;
            }

            if (item.isFolder()) {
                icon.setImageResource(R.drawable.ic_folder);
            } else if (item.appKey != null && appsByKey.get(item.appKey) != null) {
                appRepository.getIconCache().loadInto(appsByKey.get(item.appKey), icon);
            } else {
                icon.setImageResource(R.drawable.ic_app_placeholder);
            }

            if (editMode) {
                WiggleAnimation.start(icon);
            } else {
                WiggleAnimation.stop(icon);
            }

            remove.setOnClickListener(v -> layoutRepository.removeFromHome(location));

            slot.setOnClickListener(v -> {
                if (!editMode) {
                    onItemClick(item, icon);
                }
            });

            final HomeItem dockItem = item;
            slot.setOnLongClickListener(v -> {
                boolean alreadyEditing = editMode;
                onItemLongClick(dockItem, location, v);
                if (alreadyEditing) {
                    ClipData.Item clipItem =
                            new ClipData.Item(dockItem.id + "\n" + location.encode());
                    ClipData data = new ClipData(
                            dockItem.id,
                            new String[]{AppGridAdapter.DRAG_MIME, ClipDescription.MIMETYPE_TEXT_PLAIN},
                            clipItem);
                    View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        v.startDragAndDrop(data, shadow, null, 0);
                    } else {
                        v.startDrag(data, shadow, null, 0);
                    }
                }
                return true;
            });
        }
    }

    private void setEditMode(boolean enabled) {
        editMode = enabled;
        editDone.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) {
            cancelPageFlip();
        }
        pagerAdapter.refreshEditMode();
        bindDock(layoutRepository.getLayout());
        updateDefaultLauncherBanner();
    }

    private void openSearch() {
        if (editMode || (quickPanel != null && quickPanel.isOpen())) {
            return;
        }
        searchOpen = true;
        searchOverlay.setVisibility(View.VISIBLE);
        searchOverlay.setAlpha(0f);
        searchOverlay.animate().alpha(1f).setDuration(180).start();
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
        }
        runSearch(searchInput.getText() != null ? searchInput.getText().toString() : "");
    }

    private void closeSearch() {
        searchOpen = false;
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
        }
        searchOverlay.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            searchOverlay.setVisibility(View.GONE);
            searchInput.setText("");
        }).start();
    }

    private void runSearch(@NonNull String query) {
        SearchController.Results results = SearchController.search(
                this,
                query,
                appRepository.getApps(),
                layoutRepository.getLayout().hiddenAppKeys);
        searchAdapter.submit(results);
    }

    private void openLibraryPage() {
        if (editMode || searchOpen || (quickPanel != null && quickPanel.isOpen())) {
            return;
        }
        homePager.setCurrentItem(pagerAdapter.getLibraryPageIndex(), true);
    }

    private void openLauncherSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void handleDoubleTapLock() {
        if (editMode || searchOpen || (quickPanel != null && quickPanel.isOpen())) {
            return;
        }
        if (FeaturePreferences.canLockScreen(this)) {
            if (!LockAccessibilityService.lockScreen()) {
                Toast.makeText(this, R.string.lock_failed, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        startActivity(new Intent(this, LockOptInActivity.class));
    }

    @Override
    public boolean gesturesEnabled() {
        return !editMode && !searchOpen && (quickPanel == null || !quickPanel.isOpen());
    }

    @Override
    public void onSwipeDownSearch() {
        openSearch();
    }

    @Override
    public void onSwipeDownQuickPanel() {
        if (!editMode && !searchOpen && quickPanel != null) {
            quickPanel.open();
        }
    }

    @Override
    public void onSwipeUpLibrary() {
        openLibraryPage();
    }

    @Override
    public void onDoubleTapLock() {
        handleDoubleTapLock();
    }

    @Override
    public void onPinchSettings() {
        if (!editMode && !searchOpen && (quickPanel == null || !quickPanel.isOpen())) {
            openLauncherSettings();
        }
    }

    @Override
    public boolean isEditMode() {
        return editMode;
    }

    @Override
    public void onAddWidget(int pageIndex) {
        pendingWidgetPage = pageIndex;
        pendingStackTarget = null;
        showWidgetPicker();
    }

    @Override
    public void onRemoveWidget(@NonNull WidgetPlacement placement) {
        for (Integer id : placement.appWidgetIds) {
            widgetHostManager.deleteAppWidgetId(id);
        }
        layoutRepository.removeWidget(placement.id);
    }

    @Override
    public void onResizeWidget(@NonNull WidgetPlacement placement, int spanRows) {
        WidgetPlacement updated = placement.copy();
        updated.spanRows = spanRows;
        layoutRepository.updateWidget(updated);
    }

    @Override
    public void onWidgetLongClick(
            int pageIndex,
            @NonNull WidgetPlacement placement,
            @NonNull View anchor) {
        if (!editMode) {
            setEditMode(true);
        }
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.widget_add_to_stack);
        menu.getMenu().add(0, 2, 1, R.string.widget_resize_taller);
        menu.getMenu().add(0, 3, 2, R.string.widget_resize_shorter);
        menu.getMenu().add(0, 4, 3, R.string.widget_remove);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                pendingWidgetPage = pageIndex;
                pendingStackTarget = placement;
                showWidgetPicker();
                return true;
            }
            if (id == 2) {
                onResizeWidget(placement, Math.min(4, placement.spanRows + 1));
                return true;
            }
            if (id == 3) {
                onResizeWidget(placement, Math.max(2, placement.spanRows - 1));
                return true;
            }
            if (id == 4) {
                onRemoveWidget(placement);
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void showWidgetPicker() {
        List<WidgetInstaller.Option> options = widgetInstaller.listOptions(this);
        CharSequence[] labels = new CharSequence[options.size()];
        for (int i = 0; i < options.size(); i++) {
            WidgetInstaller.Option option = options.get(i);
            labels[i] = option.builtin
                    ? option.label
                    : option.label + " · " + option.provider.getPackageName();
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.widget_pick_title)
                .setItems(labels, (dialog, which) ->
                        beginInstall(options.get(which).provider))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void beginInstall(@NonNull ComponentName provider) {
        widgetInstaller.install(provider, new WidgetInstaller.Callbacks() {
            @Override
            public void onWidgetReady(int appWidgetId) {
                placeReadyWidget(appWidgetId);
            }

            @Override
            public void onWidgetFailed(@NonNull String reason) {
                int message = "configure".equals(reason)
                        ? R.string.widget_configure_failed
                        : R.string.widget_bind_denied;
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                pendingStackTarget = null;
            }
        });
    }

    private void placeReadyWidget(int appWidgetId) {
        if (pendingStackTarget != null) {
            WidgetPlacement stack = pendingStackTarget.copy();
            stack.appWidgetIds.add(appWidgetId);
            // Reconstruct as stack placement (stack flag + same id).
            WidgetPlacement updated = new WidgetPlacement(
                    stack.id,
                    true,
                    stack.appWidgetIds,
                    stack.spanRows,
                    stack.rank);
            layoutRepository.updateWidget(updated);
            pendingStackTarget = null;
            return;
        }
        WidgetPlacement placement = WidgetPlacement.single(appWidgetId, 2, 0);
        layoutRepository.addWidgetToPage(pendingWidgetPage, placement);
    }

    @Override
    public void onDragStarted() {
    }

    private void maybeFlipPageForDrag(float x, int width) {
        if (width <= 0) {
            return;
        }
        float edge = width * 0.12f;
        if (x < edge) {
            schedulePageFlip(-1);
        } else if (x > width - edge) {
            schedulePageFlip(1);
        } else {
            cancelPageFlip();
        }
    }

    private void schedulePageFlip(int delta) {
        if (pendingPageFlip != null) {
            return;
        }
        pendingPageFlip = () -> {
            pendingPageFlip = null;
            int next = homePager.getCurrentItem() + delta;
            int max = Math.max(0, pagerAdapter.getLibraryPageIndex() - 1);
            if (next >= 0 && next <= max) {
                homePager.setCurrentItem(next, true);
            }
        };
        pageFlipHandler.postDelayed(pendingPageFlip, 450);
    }

    private void cancelPageFlip() {
        if (pendingPageFlip != null) {
            pageFlipHandler.removeCallbacks(pendingPageFlip);
            pendingPageFlip = null;
        }
    }

    @Override
    public void onItemClick(@NonNull HomeItem item, @NonNull View anchor) {
        if (item.isFolder()) {
            FolderPopup.show(
                    this,
                    item,
                    appsByKey,
                    appRepository.getIconCache(),
                    layoutRepository);
            return;
        }
        launchHomeItem(item, anchor);
    }

    private void launchHomeItem(@NonNull HomeItem item, @NonNull View anchor) {
        if (!item.isApp() || item.appKey == null) {
            return;
        }
        AppInfo app = appsByKey.get(item.appKey);
        if (app == null) {
            return;
        }
        if (AppLockPreferences.isLocked(this, item.appKey)) {
            AppLockHelper.authenticate(this, app.label, new AppLockHelper.Callback() {
                @Override
                public void onAuthenticated() {
                    AppGridAdapter.launchApp(anchor, item, appsByKey);
                }

                @Override
                public void onFailed() {
                    Toast.makeText(MainActivity.this, R.string.app_lock_failed, Toast.LENGTH_SHORT)
                            .show();
                }
            });
            return;
        }
        AppGridAdapter.launchApp(anchor, item, appsByKey);
    }

    @Override
    public void onItemLongClick(
            @NonNull HomeItem item,
            @NonNull ItemLocation location,
            @NonNull View anchor) {
        setEditMode(true);
        showItemMenu(item, location, anchor);
    }

    @Override
    public void onRemoveClick(@NonNull ItemLocation location) {
        layoutRepository.removeFromHome(location);
    }

    @Override
    public void onItemDropped(@NonNull ItemLocation from, @NonNull ItemLocation to) {
        cancelPageFlip();
        int capacity = HomePreferences.pageCapacity(this);
        layoutRepository.moveOrMerge(from, to, capacity);
    }

    private void showItemMenu(
            @NonNull HomeItem item,
            @NonNull ItemLocation location,
            @NonNull View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        if (item.isFolder()) {
            menu.getMenu().add(0, 1, 0, R.string.action_rename_folder);
            menu.getMenu().add(0, 2, 1, R.string.remove_from_home);
        } else {
            menu.getMenu().add(0, 3, 0, R.string.action_app_info);
            menu.getMenu().add(0, 4, 1, R.string.action_uninstall);
            menu.getMenu().add(0, 5, 2, R.string.action_hide);
            boolean locked = item.appKey != null && AppLockPreferences.isLocked(this, item.appKey);
            menu.getMenu().add(0, 6, 3, locked ? R.string.action_unlock_app : R.string.action_lock_app);
            menu.getMenu().add(0, 2, 4, R.string.remove_from_home);
        }
        menu.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == 1) {
                FolderPopup.show(
                        this,
                        item,
                        appsByKey,
                        appRepository.getIconCache(),
                        layoutRepository);
                return true;
            }
            if (id == 2) {
                layoutRepository.removeFromHome(location);
                return true;
            }
            if (id == 3 && item.appKey != null) {
                AppInfo app = appsByKey.get(item.appKey);
                if (app != null) {
                    AppActions.openAppInfo(this, app);
                }
                return true;
            }
            if (id == 4 && item.appKey != null) {
                AppInfo app = appsByKey.get(item.appKey);
                if (app != null) {
                    AppActions.uninstall(this, app);
                }
                return true;
            }
            if (id == 5 && item.appKey != null) {
                layoutRepository.hideApp(item.appKey);
                Toast.makeText(this, R.string.app_hidden, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (id == 6 && item.appKey != null) {
                boolean locked = AppLockPreferences.isLocked(this, item.appKey);
                AppLockPreferences.setLocked(this, item.appKey, !locked);
                Toast.makeText(
                        this,
                        locked ? R.string.app_unlocked : R.string.app_locked,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
        menu.show();
    }

    private void updateDefaultLauncherBanner() {
        boolean isDefault = DefaultLauncherHelper.isDefaultLauncher(this);
        boolean hide = isDefault
                || editMode
                || searchOpen
                || (quickPanel != null && quickPanel.isOpen());
        defaultBanner.setVisibility(hide ? View.GONE : View.VISIBLE);
    }
}
