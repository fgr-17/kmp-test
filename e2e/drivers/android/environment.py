from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from common.cloud import CloudClient
from drivers.android.android_driver import AndroidDriver


def before_all(context) -> None:
    context.cloud = CloudClient()
    context.driver = AndroidDriver()


def after_all(context) -> None:
    driver = getattr(context, "driver", None)
    if driver is not None:
        driver.close()
