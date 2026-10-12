# Asset licenses

All third-party and first-party assets used in Aero Launcher are listed here.
No Apple / iPhone / iOS trademarks, icons, wallpapers, fonts, or sounds are used.

## Inter font

| Item | Detail |
| --- | --- |
| Files | `app/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter.xml` |
| License file | `app/src/main/assets/licenses/OFL.txt` |
| License | SIL Open Font License 1.1 |
| Source | [rsms/inter](https://github.com/rsms/inter) |
| Notes | App typeface only. Free and open-licensed. |

## Launcher / splash icons

| Item | Detail |
| --- | --- |
| Files | `ic_launcher.xml`, `ic_launcher_round.xml`, `ic_launcher_foreground.xml`, `ic_launcher_background.xml`, `ic_splash.xml` under `app/src/main/res/drawable*` |
| License | Original work for Aero Launcher |
| Notes | Adaptive / vector placeholders. Replace with store-ready 512×512 PNG before Play release (see `RELEASE_CHECKLIST.md`). |

## UI icons (vector)

| Item | Detail |
| --- | --- |
| Files | `ic_settings_gear.xml`, `ic_folder.xml`, `ic_app_placeholder.xml`, `ic_remove_badge.xml`, `ic_web_search.xml`, `ic_flashlight.xml`, `ic_widget_taller.xml`, `ic_widget_shorter.xml`, `page_dot.xml`, `widget_stack_dot.xml` |
| License | Original work for Aero Launcher |
| Notes | Simple geometric vectors drawn for this project. |

## Glass / shape / palette drawables

| Item | Detail |
| --- | --- |
| Files | `bg_glass_card.xml`, `bg_glass_panel.xml`, `bg_builtin_widget.xml`, `bg_notification_badge.xml`, `swatch_midnight.xml`, `swatch_violet.xml`, `swatch_aqua.xml`, `swatch_orange.xml` |
| License | Original work for Aero Launcher |

## Built-in wallpapers

| Item | Detail |
| --- | --- |
| Files | `wallpaper_dusk.xml`, `wallpaper_aurora.xml`, `wallpaper_mist.xml`, `wallpaper_ember.xml` |
| License | Original work for Aero Launcher |
| Notes | Gradient shapes only; not based on any third-party photography or brand imagery. |

## Audit summary (store readiness)

| Check | Result |
| --- | --- |
| Every bundled asset listed above | Yes |
| “iPhone” / “iOS” / “Apple” product text in app sources | None found (only a project rule warning against Apple branding) |
| `QUERY_ALL_PACKAGES` | Not declared; discovery uses `<queries>` for `MAIN`/`LAUNCHER`, `WEB_SEARCH`, and `APPWIDGET_UPDATE` |
| Permissions | Only those required by shipped features (see `RELEASE_CHECKLIST.md`) |
