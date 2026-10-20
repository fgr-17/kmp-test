Feature: Offline field notes sync
  As a cellar operator
  I capture notes while offline and sync them when connectivity returns.
  Every client app (web, android, ios, desktop) runs this same feature
  through its own Behave driver.

  Background:
    Given the cloud backend is reachable

  Scenario: Add note while offline stays pending on device
    Given the app is offline
    When I add a note "Checked barrel B12"
    Then I should see a pending note "Checked barrel B12"
    And the cloud should not contain "Checked barrel B12"

  Scenario: Sync pushes pending notes to the cloud
    Given the app is offline
    And I add a note "Checked barrel B12"
    When I go online
    And I sync notes
    Then I should see a synced note "Checked barrel B12"
    And the cloud should contain "Checked barrel B12"
