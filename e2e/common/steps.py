from __future__ import annotations

from behave import given, then, when


@given("the cloud backend is reachable")
def step_cloud_reachable(context) -> None:
    try:
        payload = context.cloud.health()
    except Exception as exc:  # noqa: BLE001 — surface a short operator hint
        raise AssertionError(
            f"Cloud backend not reachable at {context.cloud.base_url}/health.\n"
            f"Start it with:  cd backend && go run .\n"
            f"Or:             docker compose up --build api\n"
            f"Underlying error: {exc}"
        ) from None
    assert payload.get("status") == "ok", payload


@given("the app is offline")
def step_app_offline(context) -> None:
    context.driver.set_online(False)


@when("I go online")
def step_go_online(context) -> None:
    context.driver.set_online(True)


@when('I add a note "{text}"')
@given('I add a note "{text}"')
def step_add_note(context, text: str) -> None:
    context.driver.add_note(text)


@when("I sync notes")
def step_sync(context) -> None:
    context.driver.sync_notes()


@then('I should see a pending note "{text}"')
def step_see_pending(context, text: str) -> None:
    notes = context.driver.visible_notes()
    assert any(status.upper() == "PENDING" and body == text for status, body in notes), notes


@then('I should see a synced note "{text}"')
def step_see_synced(context, text: str) -> None:
    notes = context.driver.visible_notes()
    assert any(status.upper() == "SYNCED" and body == text for status, body in notes), notes


@then('the cloud should contain "{text}"')
def step_cloud_contains(context, text: str) -> None:
    assert context.cloud.contains_text(text), context.cloud.list_notes()


@then('the cloud should not contain "{text}"')
def step_cloud_missing(context, text: str) -> None:
    assert not context.cloud.contains_text(text), context.cloud.list_notes()
