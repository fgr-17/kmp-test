package main

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"
)

type Note struct {
	ID        string `json:"id"`
	Text      string `json:"text"`
	CreatedAt string `json:"createdAt"`
}

type batchRequest struct {
	Notes []Note `json:"notes"`
}

type batchResponse struct {
	AcceptedIDs []string `json:"acceptedIds"`
}

type store struct {
	mu    sync.RWMutex
	notes map[string]Note
}

func newStore() *store {
	return &store{notes: make(map[string]Note)}
}

func (s *store) upsertAll(notes []Note) []string {
	s.mu.Lock()
	defer s.mu.Unlock()
	accepted := make([]string, 0, len(notes))
	for _, n := range notes {
		if n.ID == "" || n.Text == "" {
			continue
		}
		s.notes[n.ID] = n
		accepted = append(accepted, n.ID)
	}
	return accepted
}

func (s *store) list() []Note {
	s.mu.RLock()
	defer s.mu.RUnlock()
	out := make([]Note, 0, len(s.notes))
	for _, n := range s.notes {
		out = append(out, n)
	}
	return out
}

func withCORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type")
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func main() {
	s := newStore()
	mux := http.NewServeMux()

	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, map[string]string{
			"status": "ok",
			"time":   time.Now().UTC().Format(time.RFC3339),
		})
	})

	mux.HandleFunc("GET /notes", func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, s.list())
	})

	mux.HandleFunc("POST /notes/batch", func(w http.ResponseWriter, r *http.Request) {
		var body batchRequest
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			http.Error(w, "invalid json", http.StatusBadRequest)
			return
		}
		accepted := s.upsertAll(body.Notes)
		log.Printf("synced %d note(s)", len(accepted))
		writeJSON(w, http.StatusOK, batchResponse{AcceptedIDs: accepted})
	})

	addr := ":8081"
	log.Printf("GreetingKMP cloud backend listening on http://localhost%s", addr)
	log.Fatal(http.ListenAndServe(addr, withCORS(mux)))
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}
