package com.yourstudio.aerolauncher;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.yourstudio.aerolauncher.launcher.AppActions;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppRepository;
import com.yourstudio.aerolauncher.launcher.DefaultLauncherHelper;
import com.yourstudio.aerolauncher.layout.HomeItem;
import com.yourstudio.aerolauncher.layout.HomeLayout;
import com.yourstudio.aerolauncher.layout.ItemLocation;
import com.yourstudio.aerolauncher.layout.LayoutRepository;
import com.yourstudio.aerolauncher.ui.AppGridAdapter;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;
import com.yourstudio.aerolauncher.ui.FolderPopup;
import com.yourstudio.aerolauncher.ui.HomeDragListener;
import com.yourstudio.aerolauncher.ui.HomePagerAdapter;
import com.yourstudio.aerolauncher.ui.PageDotsController;
import com.yourstudio.aerolauncher.ui.WiggleAnimation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home screen launcher with edit mode, folders, and Room-backed layout.
 */
public class MainActivity extends AppCompatActivity
        implements AppRepository.Listener, LayoutRepository.Listener, AppGridAdapter.Callbacks {

    private AppRepository appRepository;
    private LayoutRepository layoutRepository;
    private HomePagerAdapter pagerAdapter;
    private PageDotsController pageDots;
    private ViewPager2 homePager;
    private LinearLayout dockRow;
    private MaterialCardView defaultBanner;
    private MaterialButton editDone;
    private final Map<String, AppInfo> appsByKey = new HashMap<>();
    private boolean keepSplash = true;
    private boolean editMode;
    private final Handler pageFlipHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingPageFlip;

    private final ActivityResultLauncher<Intent> homeRoleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                    updateDefaultLauncherBanner());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> keepSplash);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EdgeToEdge.apply(this, findViewById(R.id.root));

        AeroLauncherApp app = (AeroLauncherApp) getApplication();
        appRepository = app.getAppRepository();
        layoutRepository = app.getLayoutRepository();

        homePager = findViewById(R.id.home_pager);
        dockRow = findViewById(R.id.dock_row);
        defaultBanner = findViewById(R.id.default_launcher_banner);
        editDone = findViewById(R.id.edit_done);
        pageDots = new PageDotsController(findViewById(R.id.page_dots));
        pagerAdapter = new HomePagerAdapter(appRepository.getIconCache(), appsByKey);
        pagerAdapter.setCallbacks(this);
        homePager.setAdapter(pagerAdapter);
        homePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pageDots.setSelected(position);
            }
        });
        // Flip pages when a drag hovers near the left/right edge.
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

        findViewById(R.id.set_default_launcher).setOnClickListener(v ->
                DefaultLauncherHelper.requestDefaultLauncher(this, homeRoleLauncher));
        findViewById(R.id.open_settings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        editDone.setOnClickListener(v -> setEditMode(false));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
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
    protected void onResume() {
        super.onResume();
        updateDefaultLauncherBanner();
        // Re-sync only when the grid capacity changed in Settings.
        HomeLayout current = layoutRepository.getLayout();
        int capacity = HomePreferences.pageCapacity(this);
        if (!appsByKey.isEmpty()
                && !current.pages.isEmpty()
                && current.pages.get(0).size() != capacity) {
            layoutRepository.loadAndSync(appRepository.getApps());
        }
    }

    @Override
    protected void onDestroy() {
        appRepository.removeListener(this);
        layoutRepository.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onAppsUpdated(@NonNull List<AppInfo> apps) {
        appsByKey.clear();
        for (AppInfo appInfo : apps) {
            appsByKey.put(appInfo.key, appInfo);
        }
        layoutRepository.loadAndSync(apps);
    }

    @Override
    public void onLayoutChanged(@NonNull HomeLayout layout) {
        keepSplash = false;
        bindLayout(layout);
        updateDefaultLauncherBanner();
    }

    private void bindLayout(@NonNull HomeLayout layout) {
        int columns = HomePreferences.getColumns(this);
        int previousPage = homePager.getCurrentItem();
        pagerAdapter.setPages(layout.pages, columns);
        int pageCount = pagerAdapter.getPageCount();
        pageDots.setPageCount(pageCount);
        if (previousPage >= pageCount) {
            homePager.setCurrentItem(pageCount - 1, false);
        }
        pageDots.setSelected(homePager.getCurrentItem());
        bindDock(layout);
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

    @Override
    public boolean isEditMode() {
        return editMode;
    }

    @Override
    public void onDragStarted() {
        // Page edge auto-flip handles cross-page moves while dragging.
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
            if (next >= 0 && next < pagerAdapter.getPageCount()) {
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
            menu.getMenu().add(0, 2, 3, R.string.remove_from_home);
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
            return false;
        });
        menu.show();
    }

    private void updateDefaultLauncherBanner() {
        boolean isDefault = DefaultLauncherHelper.isDefaultLauncher(this);
        defaultBanner.setVisibility(isDefault || editMode ? View.GONE : View.VISIBLE);
    }
}
