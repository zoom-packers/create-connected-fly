"""Convert Fabric resource conditions in data JSON to NeoForge's (NeoForge 26.3 port).

    python -I tools/fabric_conditions_to_neoforge.py src/generated/resources src/main/resources

"fabric:load_conditions": [...] becomes "neoforge:conditions": [...]. Each entry's "condition" discriminator becomes
"type"; fabric:all_mods_loaded / any_mod_loaded become neoforge:mod_loaded (wrapped in neoforge:and / neoforge:or when
there are several mods), fabric:not / fabric:or / fabric:and become their neoforge: equivalents. Other condition types
(create_connected:*) keep their fields. Idempotent.
"""
import json
import pathlib
import sys


def cond(c):
    t = c["condition"]
    rest = {k: v for k, v in c.items() if k != "condition"}
    if t in ("fabric:all_mods_loaded", "fabric:any_mod_loaded"):
        mods = [{"type": "neoforge:mod_loaded", "modid": m} for m in rest["values"]]
        if len(mods) == 1:
            return mods[0]
        return {"type": "neoforge:and" if t == "fabric:all_mods_loaded" else "neoforge:or", "values": mods}
    if t == "fabric:not":
        return {"type": "neoforge:not", "value": cond(rest["value"])}
    if t in ("fabric:or", "fabric:and"):
        return {"type": "neoforge:" + t.split(":")[1], "values": [cond(v) for v in rest["values"]]}
    if t.startswith("fabric:"):
        raise SystemExit(f"unhandled fabric condition {t}")
    return {"type": t, **rest}


def walk(o):
    if isinstance(o, dict):
        out = {}
        for k, v in o.items():
            if k == "fabric:load_conditions":
                out["neoforge:conditions"] = [cond(c) for c in v]
            else:
                out[k] = walk(v)
        return out
    if isinstance(o, list):
        return [walk(v) for v in o]
    return o


n = 0
for d in sys.argv[1:]:
    for f in pathlib.Path(d).rglob("*.json"):
        raw = f.read_bytes().decode("utf-8")
        if "fabric:load_conditions" not in raw:
            continue
        data = walk(json.loads(raw))
        f.write_text(json.dumps(data, indent=2, ensure_ascii=False) + ("\n" if raw.endswith("\n") else ""), encoding="utf-8", newline="\n")
        n += 1
print(f"{n} files converted")
