# Postman local checks

1. Start MongoDB and the backend using the local profile.
2. Import `CV-Analyzer-Local.postman_collection.json` into Postman.
3. Keep `baseUrl` as `http://localhost:8080`, or change the collection variable if the backend uses another port.
4. Keep the collection variable allowWrites set to false for read-only use. Every POST is skipped
   by a pre-request script in this mode.
5. Run the collection in order. The list request stores the first real job ID for the details request.

The collection expects HTTP 200 from all four endpoints and validates their basic response shapes. Run the ingestion request a second time to inspect idempotency: unchanged jobs should be reported as `skipped`, not inserted again.

Command-line execution is also possible after installing Newman:

```powershell
newman run .\postman\CV-Analyzer-Local.postman_collection.json
```

Health, List jobs, and Get job details are read-only. The three provider ingestion requests make
external calls and write MongoDB. Classify existing jobs writes MongoDB but performs no external
call. To run a write intentionally, first enable backend ingestion when applicable, review the
provider budget, and then set allowWrites to true. Restore it to false afterward.

Provider credentials stay in backend environment variables and are never stored in Postman.
No Newman/Postman execution is implied by merely importing the collection.
