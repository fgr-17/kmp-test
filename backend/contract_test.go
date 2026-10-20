package main

import (
	"bytes"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"runtime"
	"testing"
)

func fixturesDir(t *testing.T) string {
	t.Helper()
	_, file, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("runtime.Caller failed")
	}
	return filepath.Clean(filepath.Join(filepath.Dir(file), "..", "contracts", "fixtures"))
}

func TestBatchContractFixture(t *testing.T) {
	reqBytes, err := os.ReadFile(filepath.Join(fixturesDir(t), "batch_request.json"))
	if err != nil {
		t.Fatalf("read fixture: %v", err)
	}
	wantRespBytes, err := os.ReadFile(filepath.Join(fixturesDir(t), "batch_response.json"))
	if err != nil {
		t.Fatalf("read fixture: %v", err)
	}

	s := newStore()
	mux := newMux(s)

	req := httptest.NewRequest(http.MethodPost, "/notes/batch", bytes.NewReader(reqBytes))
	rr := httptest.NewRecorder()
	mux.ServeHTTP(rr, req)

	if rr.Code != http.StatusOK {
		t.Fatalf("status=%d body=%s", rr.Code, rr.Body.String())
	}

	var got batchResponse
	if err := json.Unmarshal(rr.Body.Bytes(), &got); err != nil {
		t.Fatalf("decode response: %v", err)
	}
	var want batchResponse
	if err := json.Unmarshal(wantRespBytes, &want); err != nil {
		t.Fatalf("decode want: %v", err)
	}
	if len(got.AcceptedIDs) != len(want.AcceptedIDs) || got.AcceptedIDs[0] != want.AcceptedIDs[0] {
		t.Fatalf("acceptedIds got=%v want=%v", got.AcceptedIDs, want.AcceptedIDs)
	}
}

func TestDeleteNotesEmptiesStore(t *testing.T) {
	s := newStore()
	s.upsertAll([]Note{{ID: "note-1", Text: "Checked barrel B12", CreatedAt: "2026-01-01T00:00:00Z"}})

	rr := httptest.NewRecorder()
	newMux(s).ServeHTTP(rr, httptest.NewRequest(http.MethodDelete, "/notes", nil))
	if rr.Code != http.StatusNoContent {
		t.Fatalf("status=%d", rr.Code)
	}
	if len(s.list()) != 0 {
		t.Fatalf("store still has %#v", s.list())
	}
}
