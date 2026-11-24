# Multilingual Column Translation Design

## Translation provider choice

The feature uses a self-hosted [LibreTranslate](https://libretranslate.com/) instance. It is configured by an endpoint URL and needs no API key, so the implementation has no credentials and no paid provider in its default path.

| Option | Cost and limits | Setup | Speed and quality | Outcome |
|---|---|---|---|---|
| Google Cloud Translation, DeepL, or a hosted LLM | Usually metered; usage limits and credentials vary | Account, key handling, billing setup | Often fast and high quality | Rejected: users may spend money and must manage credentials. |
| Public LibreTranslate instances | Free instances commonly impose rate limits and may change availability | No local setup | Speed and availability vary | Not made the default because grading and use can be interrupted by shared limits. |
| Local Argos Translate | No API cost; local model files | Python setup and model downloads | Offline, but models and language coverage vary | Rejected for the default path: a uniform HTTP service is easier to integrate and run. |
| **Local LibreTranslate** | No API cost; limits are the machine's resources | One Docker command | Network-local and deterministic enough for tests; quality depends on the installed models | **Selected**: easy setup, no credentials, and supports English, Spanish, and Mandarin Chinese. |

OpenRefine still accepts another HTTP or HTTPS LibreTranslate endpoint so a self-hosted instance can run on another machine without changing code.

## Setup

Start a local server with only the required languages:

```bash
docker run --rm -p 5000:5000 libretranslate/libretranslate:latest --load-only en,es,zh
```

Then open a project, choose **Edit column → Translate column…**, and use the default endpoint:

```text
http://localhost:5000/translate
```

The endpoint, request timeout, and retry count can be changed in the dialog. No API key is requested, sent, or stored. Alternative setup without Docker is documented in the LibreTranslate project README.

## Behavior

- Supported languages are English (`en`), Spanish (`es`), and Mandarin Chinese (`zh`), each as source or target.
- The source and target languages are selected explicitly; language is not detected automatically.
- The source and target language must differ.
- Results are written to a new column, normally placed immediately after the source column. The source column, other columns, row order, and row count are preserved.
- The target column name must not already exist. The dialog warns before submission, and the backend refuses the operation as a final check.
- Blank cells, null cells, and existing error cells are not sent to the server and remain blank in the new column.
- One translation request is made per eligible source cell in project-row order. A failed cell becomes an `EvalError`; processing continues with later cells.
- All completed results and error markers are committed in one history entry. Undo removes the new column; redo restores it.
- Cancelation stops further requests and does not commit partial results.
- Retries are bounded (0–5 additional attempts). Network failures and 429/5xx responses are retried; other client errors fail immediately for that cell.

## Data for trying the feature

`Assignment3/yelp_reviews_multilingual_sample.csv` contains 20 Yelp-style reviews in English, Spanish, and Mandarin Chinese. Import it into OpenRefine, start LibreTranslate, select the `review_text` column, and choose a source/target language pair matching a row group.
