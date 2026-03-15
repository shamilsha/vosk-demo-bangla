#!/usr/bin/env python3
"""Generate SV_Xing.txt (present continuous) and SV_X_past_cont.txt (past continuous) from SV_Ing.txt and SV_*.txt."""
import os
import re

ASSETS_SVO = os.path.dirname(os.path.abspath(__file__))

def parse_line(line):
    line = line.strip()
    if not line:
        return None
    parts = [p.strip() for p in line.split(",", 2)]
    if len(parts) < 2:
        return None
    eng, bn = parts[0], parts[1]
    pron = parts[2] if len(parts) > 2 else ""
    return (eng, bn, pron)

def get_verb_ing(eng_i_am):
    """From 'I am playing' return 'playing'."""
    if eng_i_am.startswith("I am "):
        return eng_i_am[5:]  # "playing"
    return ""

def bengali_present_cont_for_subject(bn_i, subject):
    """From Bengali 'আমি খেলছি' (I am playing) return Bengali for given subject."""
    # I: আমি Xছি -> We: আমরা Xছি, He/She/They: সে/তারা Xছে, You: তুমি Xছো
    if subject == "I":
        return bn_i
    if subject == "We":
        return bn_i.replace("আমি ", "আমরা ", 1)
    if subject in ("He", "She"):
        s = bn_i.replace("আমি ", "সে ", 1)
        if s.endswith("ছি"):
            s = s[:-1] + "ে"  # ছি -> ছে
        return s
    if subject == "They":
        s = bn_i.replace("আমি ", "তারা ", 1)
        if s.endswith("ছি"):
            s = s[:-1] + "ে"
        return s
    if subject == "You":
        s = bn_i.replace("আমি ", "তুমি ", 1)
        if s.endswith("ছি"):
            s = s[:-1] + "ো"  # ছি -> ছো
        return s
    return bn_i

def bengali_past_cont_from_present(bn_present_cont):
    """Past continuous from present continuous. খেলছি->খেলছিলাম, খেলছে->খেলছিল, খেলছো->খেলছিলে."""
    s = bn_present_cont
    if s.endswith("ছি"):
        s = s[:-1] + "িলাম"   # ছি -> ছিলাম
    elif s.endswith("ছে"):
        s = s[:-1] + "িল"     # ছে -> ছিল
    elif s.endswith("ছো"):
        s = s[:-1] + "িলে"    # ছো -> ছিলে
    return s

def pron_for_subject_present(pron_i_am, subject):
    """Approximate pronunciation: I am playing -> He is playing (হি ইজ প্লেয়িং). Third column is translit."""
    if subject == "I":
        return pron_i_am
    # Strip leading "আই অ্যাম " or similar and add subject+aux
    p = re.sub(r"^[^\s]+\s+[^\s]+\s+", "", pron_i_am, count=1)  # drop first two words (I am)
    prefix = {"He": "হি ইজ ", "She": "শি ইজ ", "You": "ইউ আর ", "We": "উই আর ", "They": "দে আর "}
    return prefix.get(subject, "") + p

def pron_past_cont(pron_present, subject):
    """Was/were + verbing. pron_present e.g. 'হি ইজ প্লেয়িং' -> 'হি ওয়াজ প্লেয়িং'."""
    parts = pron_present.split(None, 2)
    if len(parts) < 3:
        return pron_present
    subj_pr, aux, rest = parts[0], parts[1], parts[2]
    was_were = "ওয়াজ" if subject in ("I", "He", "She") else "ওয়ার"
    return f"{subj_pr} {was_were} {rest}"

def main():
    with open(os.path.join(ASSETS_SVO, "SV_Ing.txt"), "r", encoding="utf-8") as f:
        ing_lines = [parse_line(l) for l in f if parse_line(l)]

    subjects = ["I", "He", "She", "You", "We", "They"]
    aux_present = {"I": "am", "He": "is", "She": "is", "You": "are", "We": "are", "They": "are"}
    aux_past = "was" if True else "were"  # I/He/She/It was; You/We/They were
    aux_past_map = {"I": "was", "He": "was", "She": "was", "You": "were", "We": "were", "They": "were"}

    # Present continuous: SV_Iing, SV_Heing, ...
    for subj in subjects:
        out_lines = []
        for (eng, bn, pron) in ing_lines:
            verb_ing = get_verb_ing(eng)
            if not verb_ing:
                continue
            eng_new = f"{subj} {aux_present[subj]} {verb_ing}"
            bn_new = bengali_present_cont_for_subject(bn, subj)
            pron_new = pron_for_subject_present(pron, subj)
            out_lines.append(f"{eng_new},{bn_new},{pron_new}")
        fname = f"SV_{subj}ing.txt" if subj != "I" else "SV_Iing.txt"
        with open(os.path.join(ASSETS_SVO, fname), "w", encoding="utf-8") as f:
            f.write("\n".join(out_lines) + "\n")
        print("Wrote", fname)

    # Past continuous: SV_I_past_cont.txt, ...
    for subj in subjects:
        out_lines = []
        for (eng, bn, pron) in ing_lines:
            verb_ing = get_verb_ing(eng)
            if not verb_ing:
                continue
            eng_new = f"{subj} {aux_past_map[subj]} {verb_ing}"
            bn_present = bengali_present_cont_for_subject(bn, subj)
            bn_past = bengali_past_cont_from_present(bn_present)
            pron_new = pron_past_cont(pron_for_subject_present(pron, subj), subj)
            out_lines.append(f"{eng_new},{bn_past},{pron_new}")
        fname = f"SV_{subj}_past_cont.txt"
        with open(os.path.join(ASSETS_SVO, fname), "w", encoding="utf-8") as f:
            f.write("\n".join(out_lines) + "\n")
        print("Wrote", fname)

if __name__ == "__main__":
    main()
