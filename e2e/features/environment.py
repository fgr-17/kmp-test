"""Behave hooks live next to features/ so they are always discovered."""

from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from common.cloud import CloudClient


def before_all(context) -> None:
    driver_name = (context.config.userdata.get("driver") or "web").lower()
    context.cloud = CloudClient()
    context.driver_name = driver_name

    if driver_name == "web":
        from drivers.web.web_driver import WebDriver

        context.driver = WebDriver()
    elif driver_name == "desktop":
        from drivers.desktop.desktop_driver import DesktopDriver

        context.driver = DesktopDriver()
    elif driver_name == "android":
        from drivers.android.android_driver import AndroidDriver

        context.driver = AndroidDriver()
    elif driver_name == "ios":
        from drivers.ios.ios_driver import IosDriver

        context.driver = IosDriver()
    else:
        raise ValueError(f"Unknown driver '{driver_name}'. Use web|android|ios|desktop.")


def before_scenario(context, scenario) -> None:  # noqa: ARG001 — behave hook signature
    context.cloud.clear()
    context.driver.reset()


def after_all(context) -> None:
    cloud = getattr(context, "cloud", None)
    if cloud is not None:
        try:
            cloud.clear()
        except Exception:
            pass
    driver = getattr(context, "driver", None)
    if driver is not None:
        driver.close()
