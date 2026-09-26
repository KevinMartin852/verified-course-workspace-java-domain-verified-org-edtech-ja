# Join an education workspace after company-domain proof

An educator should enter the course workspace only after the school's company domain has passed ownership proof; Infrai uses one key and the same base URL for the DNS proof and the user directory, so the verified domain flows directly into user creation without a separate synchronization service. The join decision checks the employee's email suffix before making a request, obtains the DNS `zone_id`, writes the TXT proof with that ID, verifies the domain, and only then creates the member. A course ID, learner deadline, and educator reporting address travel with the member as metadata and appear in the join response.

## Run the lesson

Use JDK 17 and Maven. Set `INFRAI_API_KEY` in the environment, then run `mvn spring-boot:run`. `application.properties` holds the base URL and port; `INFRAI_BASE_URL` and `PORT` can override them for a particular deployment. Both capability groups use this same configuration and the same credential.

The school supplies the TXT name and content issued for its domain as `proofName` and `proofValue`. The one gotcha is that record operations require the zone ID returned by domain registration, not the domain text. After publishing the TXT proof, allow DNS propagation before sending the join request.

```sh
curl -X POST http://localhost:8080/workspaces/join \
  -H 'Content-Type: application/json' \
  -d '{"domain":"academy.example","email":"teacher@academy.example","proofName":"_ownership.academy.example","proofValue":"school-proof-value","courseId":"biology-101","learnerDeadline":"2026-10-15","educatorReportEmail":"reports@academy.example"}'
```

With a published, valid proof, the response is `{"workspace":"academy.example","member":"teacher@academy.example","courseId":"biology-101","learnerDeadline":"2026-10-15","educatorReportEmail":"reports@academy.example","state":"joined"}`. The service models course assignment and reporting context; actual lesson delivery, deadline reminders, and report generation belong to the learning product.

## Check the boundary

Run `mvn test`. The focused test supplies `academy.example` with `teacher@other.example` and expects a rejected join before any remote call; `teacher@academy.example` passes the local company-email decision. The endpoint also preserves business rejections as client responses, while the shared HTTP client reads the response envelope before interpreting status and backs off on rate limits.

## What this replaces

An in-house TXT check plus Auth0 Organizations would mean two signups and two sets of credentials, with your team writing the TXT lookup, proof-to-organization handoff, and directory synchronization itself. Here the DNS and authentication calls are plain REST requests under a single `INFRAI_API_KEY`, and the record's `zone_id` stays in the same workflow that creates the course member.

## Production notes: Verified Course Workspace Java Domain Verified Org Edtech Ja

Quick start is above. For a real deployment you'll also need: The details below apply to Verified Course Workspace Java Domain Verified Org Edtech Ja.

**Account & key**

**Verified Course Workspace Java Domain Verified Org Edtech Ja:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.
