package com.yourstudio.aerolauncher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.card.MaterialCardView;
import com.yourstudio.aerolauncher.launcher.AppInfo;
import com.yourstudio.aerolauncher.launcher.AppLauncher;
import com.yourstudio.aerolauncher.launcher.AppRepository;
import com.yourstudio.aerolauncher.launcher.DefaultLauncherHelper;
import com.yourstudio.aerolauncher.ui.EdgeToEdge;
import com.yourstudio.aerolauncher.ui.HomePagerAdapter;
import com.yourstudio.aerolauncher.ui.PageDotsController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Home screen launcher: paged app grid, dock, and default-launcher prompt.
 */
public class MainActivity extends AppCompatActivity implements AppRepository.Listener {

    private AppRepository appRepository;
    private HomePagerAdapter pagerAdapter;
    private PageDotsController pageDots;
    private ViewPager2 homePager;
    private LinearLayout dockRow;
    private MaterialCardView defaultBanner;
    private boolean keepSplash = true;

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

        homePager = findViewById(R.id.home_pager);
        dockRow = findViewById(R.id.dock_row);
        defaultBanner = findViewById(R.id.default_launcher_banner);
        pageDots = new PageDotsController(findViewById(R.id.page_dots));
        pagerAdapter = new HomePagerAdapter(appRepository.getIconCache());
        homePager.setAdapter(pagerAdapter);
        homePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pageDots.setSelected(position);
            }
        });

        findViewById(R.id.set_default_launcher).setOnClickListener(v ->
                DefaultLauncherHelper.requestDefaultLauncher(this, homeRoleLauncher));

        ImageButton settings = findViewById(R.id.open_settings);
        settings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Launchers stay on the home screen when Back is pressed.
                if (homePager != null && homePager.getCurrentItem() != 0) {
                    homePager.setCurrentItem(0, true);
                }
            }
        });

        ensureDockSlots();
        appRepository.addListener(this);
        appRepository.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDefaultLauncherBanner();
        // Rebind so grid size changes from Settings apply immediately.
        List<AppInfo> current = appRepository.getApps();
        if (!current.isEmpty()) {
            bindHome(current);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Home button with singleTask brings us here; stay on current page.
        if (homePager != null && Intent.ACTION_MAIN.equals(intent.getAction())) {
            // No-op: already showing home.
        }
    }

    @Override
    protected void onDestroy() {
        appRepository.removeListener(this);
        super.onDestroy();
    }

    @Override
    public void onAppsUpdated(@NonNull List<AppInfo> apps) {
        keepSplash = false;
        bindHome(apps);
        updateDefaultLauncherBanner();
    }

    private void bindHome(@NonNull List<AppInfo> apps) {
        List<String> availableKeys = new ArrayList<>(apps.size());
        Map<String, AppInfo> byKey = new HashMap<>();
        for (AppInfo appInfo : apps) {
            availableKeys.add(appInfo.key);
            byKey.put(appInfo.key, appInfo);
        }

        List<String> dockKeys = HomePreferences.ensureDockDefaults(this, availableKeys);
        List<AppInfo> dockApps = new ArrayList<>(HomePreferences.DOCK_SLOTS);
        for (String key : dockKeys) {
            AppInfo dockApp = byKey.get(key);
            if (dockApp != null) {
                dockApps.add(dockApp);
            }
        }
        bindDock(dockApps);

        int columns = HomePreferences.getColumns(this);
        int capacity = HomePreferences.pageCapacity(this);
        List<List<AppInfo>> pages = paginate(apps, capacity);
        int previousPage = homePager.getCurrentItem();
        pagerAdapter.setPages(pages, columns);
        int pageCount = pagerAdapter.getPageCount();
        pageDots.setPageCount(pageCount);
        if (previousPage >= pageCount) {
            homePager.setCurrentItem(pageCount - 1, false);
        }
        pageDots.setSelected(homePager.getCurrentItem());
    }

    @NonNull
    private static List<List<AppInfo>> paginate(@NonNull List<AppInfo> apps, int capacity) {
        List<List<AppInfo>> pages = new ArrayList<>();
        if (apps.isEmpty() || capacity <= 0) {
            pages.add(new ArrayList<>());
            return pages;
        }
        for (int i = 0; i < apps.size(); i += capacity) {
            int end = Math.min(apps.size(), i + capacity);
            pages.add(new ArrayList<>(apps.subList(i, end)));
        }
        return pages;
    }

    private void ensureDockSlots() {
        dockRow.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < HomePreferences.DOCK_SLOTS; i++) {
            View slot = inflater.inflate(R.layout.item_dock_slot, dockRow, false);
            dockRow.addView(slot);
        }
    }

    private void bindDock(@NonNull List<AppInfo> dockApps) {
        for (int i = 0; i < HomePreferences.DOCK_SLOTS; i++) {
            View slot = dockRow.getChildAt(i);
            if (slot == null) {
                continue;
            }
            ImageView icon = slot.findViewById(R.id.dock_icon);
            if (i < dockApps.size()) {
                AppInfo appInfo = dockApps.get(i);
                slot.setVisibility(View.VISIBLE);
                slot.setEnabled(true);
                appRepository.getIconCache().loadInto(appInfo, icon);
                slot.setOnClickListener(v -> AppLauncher.launch(this, appInfo, icon));
                slot.setContentDescription(appInfo.label);
            } else {
                icon.setImageDrawable(null);
                slot.setOnClickListener(null);
                slot.setContentDescription(null);
            }
        }
    }

    private void updateDefaultLauncherBanner() {
        boolean isDefault = DefaultLauncherHelper.isDefaultLauncher(this);
        defaultBanner.setVisibility(isDefault ? View.GONE : View.VISIBLE);
    }
}
