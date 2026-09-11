# Extended vocabulary rebuild

`import_extended_vocabulary.py` rebuilds only the
`OPEN_VOCAB_EXTENDED_V1` rows. Existing NGSL and NAWL rows are copied without
changing their order, source ranks, or learning IDs.

The source files are intentionally not committed because they are much larger
than the generated app data. Download them from the attributed upstream pages
in `THIRD_PARTY_DATA.md`, verify the versions below, and pass their paths to the
importer.

## Pinned inputs

- EJDict-hand revision:
  `5e1a630bfabb2791a78d14e4e356d85bb6437e34`
- Open Language Profiles revision:
  `d4e45b75b38f27b30dfc5c44d8c571aec7e7092f`
- `grundwortschatz_en.db.gz` SHA-256:
  `dd0639b3f7dc96469a644cd4fff525a49f531ef2b58a3d6670601357422c9867`
- `cefrj-vocabulary-profile-1.5.csv` SHA-256:
  `b0dd3c635f1c9a4fdf1490c7e5b7c48e8bbe55b652ad0c9860a95f98e10ae498`
- `octanove-vocabulary-profile-c1c2-1.0.csv` SHA-256:
  `18c33a407f2f89f7b8de9671c6d45fe3ea0bce45e7d2d7dcaab48d73e0f7b380`
- `english-wordnet-2024.xml.gz` SHA-256:
  `e1f633b0a93758cae34ea27c44c4dad310a8af2467b155f99dd6673af697e875`
- `omw-ja-2.0.tar.xz` SHA-256:
  `dd9d1ee444d5877298083706f011fde4159f3e10cfe48b95f1c334a2a385af44`

## Required checks

After generation, run:

```sh
python3 tools/audit_words.py app/src/main/assets/words.json
python3 tools/audit_questions.py \
  app/src/main/assets/words.json \
  app/src/main/assets/question_content.json
```

The expected result is 6,700 unique words: 2,809 NGSL, 957 NAWL, and
2,934 extended words. Do not raise the target merely to reach a round number;
new rows must pass the same semantic and licensing checks first.
