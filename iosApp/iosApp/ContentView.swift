import SwiftUI
import SharedLogic
import KMPNativeCoroutinesAsync

struct ContentView: View {
    @ObservedObject private(set) var viewModel: ViewModel

    var body: some View {
        List {
            Section("Greetings") {
                ForEach(viewModel.greetings, id: \.self) { phrase in
                    Text(phrase)
                }
            }

            Section {
                Text(viewModel.status)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .accessibilityIdentifier(NotesUiTags.shared.STATUS)

                Toggle("Online", isOn: Binding(
                    get: { viewModel.isOnline },
                    set: { viewModel.setOnline($0) }
                ))
                .accessibilityIdentifier(NotesUiTags.shared.ONLINE_SWITCH)

                HStack {
                    TextField("Note (e.g. Checked barrel B12)", text: $viewModel.draft)
                        .textFieldStyle(.roundedBorder)
                        .accessibilityIdentifier(NotesUiTags.shared.NOTE_INPUT)
                    Button("Add") {
                        viewModel.addNote()
                    }
                    .accessibilityIdentifier(NotesUiTags.shared.ADD_NOTE)
                    .disabled(viewModel.draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }

                Button("Sync") {
                    Task { await viewModel.sync() }
                }
                .accessibilityIdentifier(NotesUiTags.shared.SYNC)
            } header: {
                Text("Field notes (offline → cloud)")
            }

            Section("Notes") {
                ForEach(viewModel.notes, id: \.id) { note in
                    Text("[\(note.syncStatus.name)] \(note.text)")
                }
            }
            .accessibilityIdentifier(NotesUiTags.shared.NOTES_LIST)
        }
        .task {
            await viewModel.startObserving()
        }
    }
}

extension ContentView {
    @MainActor
    class ViewModel: ObservableObject {
        @Published var greetings: [String] = []
        @Published var notes: [FieldNote] = []
        @Published var isOnline: Bool = false
        @Published var status: String = "Offline by default — add notes, then sync."
        @Published var draft: String = ""

        // Single-arg ctor (ObjC/Swift cannot see Kotlin default parameters)
        private let notesFacade = NotesFacade(baseUrl: CloudConfig.shared.IOS_SIMULATOR_BASE_URL)

        func setOnline(_ online: Bool) {
            isOnline = online
            notesFacade.isOnline = online
            status = online
                ? "Online — Sync will reach the Go backend."
                : "Offline — notes stay on device."
        }

        func addNote() {
            let text = draft.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !text.isEmpty else { return }
            notesFacade.addNote(text: text)
            draft = ""
            status = "Saved locally (\(isOnline ? "online but not synced yet" : "offline"))."
        }

        func sync() async {
            do {
                let result = try await asyncFunction(for: notesFacade.sync())
                if result is SyncResultOffline {
                    status = "Still offline — turn Online on first."
                } else if result is SyncResultNothingToSync {
                    status = "Nothing pending."
                } else if let synced = result as? SyncResultSynced {
                    status = "Synced \(synced.count) note(s) to cloud."
                } else if let failed = result as? SyncResultFailed {
                    status = "Sync failed: \(failed.message)"
                } else {
                    status = "Sync finished."
                }
            } catch {
                status = "Sync failed: \(error.localizedDescription)"
            }
        }

        func startObserving() async {
            async let greetingsTask: Void = observeGreetings()
            async let notesTask: Void = observeNotes()
            _ = await (greetingsTask, notesTask)
        }

        private func observeGreetings() async {
            do {
                let sequence = asyncSequence(for: Greeting().greet())
                for try await phrase in sequence {
                    self.greetings.append(phrase)
                }
            } catch {
                print("Failed observing greetings: \(error)")
            }
        }

        private func observeNotes() async {
            do {
                let sequence = asyncSequence(for: notesFacade.notes)
                for try await list in sequence {
                    self.notes = list
                }
            } catch {
                print("Failed observing notes: \(error)")
            }
        }
    }
}
