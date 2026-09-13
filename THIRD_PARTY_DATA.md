# Third-party word data

## NGSL 1.2

The original 2,809 English headwords are from the New General Service List 1.2.
The short Japanese quiz meanings and part-of-speech labels use the project's
Japanese Learning Dictionary.

- Authors: Charles Browne, Brent Culligan, Joseph Phillips
- Source: https://www.newgeneralservicelist.com/new-general-service-list
- Japanese learning data: https://www.linguaeruditio.com/Glossary/NGSL/JP/NGSL_jp_gloss.html
- License: Creative Commons Attribution-ShareAlike 4.0 International
- Retrieved for this project: 2026-08-30

## NAWL 1.2

The 957 added English headwords, their source ranks, short Japanese meanings,
and part-of-speech labels are derived from the New Academic Word List 1.2 data.

- Authors: Charles Browne, Brent Culligan, Joseph Phillips
- Source: https://www.newgeneralservicelist.com/new-academic-word-list
- Imported metadata: `NAWL 1.2 with English definitions and Japanese translations`
- License: Creative Commons Attribution-ShareAlike 4.0 International
- Retrieved for this project: 2026-08-30

The combined and adapted word dataset must be distributed under the same
CC BY-SA 4.0 terms with attribution.

Changes made in this project:

- converted the source lists to the app's JSON schema;
- combined NGSL and NAWL while retaining separate source identifiers and ranks;
- added Japanese dictionary entries and imported short quiz meanings and parts of speech;
- manually corrected clear source errors and disambiguated colliding quiz prompts;
- assigned app-specific school-grade, Eiken-grade, and game-level categories.

The adapted `app/src/main/assets/words.json` dataset is offered under
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
This data license does not by itself change the license of unrelated application code.
No endorsement by the original authors or licensors is implied.

## EJDict-hand

Long-form Japanese dictionary entries are taken from EJDict-hand.

- Maintainer: kujirahand
- Source: https://github.com/kujirahand/EJDict
- Revision used for the NAWL import: `5e1a630bfabb2791a78d14e4e356d85bb6437e34`
- License: CC0 / Public Domain

CC0 permits copying, modification, and redistribution, including commercially.
Attribution is retained here as a courtesy and for source traceability.
The underlying EJDict-hand entries remain available from their source under CC0;
the CC BY-SA notice for the combined dataset does not remove that status.
No endorsement by the maintainer is implied.

## Open vocabulary extension v1

The project adds 2,974 headwords under the source identifier
`OPEN_VOCAB_EXTENDED_V1`. They are not copied from an unofficial Eiken list.
For 2,956 entries, the importer intersects openly licensed lexical resources
and retains an entry only when its English part of speech and Japanese WordNet
concept match and the Japanese expression also occurs in EJDict-hand. The
remaining 18 A1 foundation entries are explicitly listed in the importer and
were manually checked against CEFR-J for headword and part of speech and
against EJDict-hand for the Japanese definition. The short prompt, app rank,
course level, and Eiken-equivalent band are project adaptations.

- WortUniversum English Vocabulary Database
  - Source: https://huggingface.co/datasets/cstr/grundwortschatz-voc-en
  - License: CC BY-SA 4.0
- Open English WordNet 2024
  - Copyright: Open English WordNet Community
  - Source: https://en-word.net/downloads
  - License: CC BY 4.0
- Japanese Wordnet 2.0
  - Copyright: 2009-2011 NICT, 2012-2015 Francis Bond, 2016-2024 Francis Bond
    and Takayuki Kuribayashi
  - Source: https://bond-lab.github.io/wnja/
  - License: WordNet-style license permitting use, copying, modification, and
    distribution when its copyright notice, statements, and disclaimer remain
    on copies
- EJDict-hand
  - Revision: `5e1a630bfabb2791a78d14e4e356d85bb6437e34`
  - License: CC0 / Public Domain

The combined adapted dataset is distributed under CC BY-SA 4.0 while each
upstream component retains its own license and attribution. No endorsement by
the source authors or organizations is implied.

## CEFR-based Eiken-equivalent classification

The app's Eiken-equivalent bands are estimates, not an official vocabulary
list or endorsement by the Eiken Foundation of Japan.

- CEFR-J Vocabulary Profile 1.5
  - Compiler: Yukio Tono Laboratory, Tokyo University of Foreign Studies
  - Source: https://github.com/openlanguageprofiles/olp-en-cefrj
  - Terms: free research and commercial use with proper citation
- Octanove Vocabulary Profile C1/C2 1.0
  - Source: https://github.com/openlanguageprofiles/olp-en-cefrj
  - License: CC BY-SA 4.0
- WortUniversum English Vocabulary Database
  - Source: https://huggingface.co/datasets/cstr/grundwortschatz-voc-en
  - License: CC BY-SA 4.0

Only the derived CEFR estimate, Eiken-equivalent band, and classification-basis
identifier are packaged with each app word. The classification was modified by
this project and may contain estimation errors.
