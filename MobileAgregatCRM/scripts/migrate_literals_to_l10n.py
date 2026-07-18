#!/usr/bin/env python3
"""Replace exact Russian UI literals with s()/tr() using strings.xml catalog."""
from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
RU_XML = ROOT / "composeApp/src/commonMain/composeResources/values/strings.xml"
UI_ROOT = ROOT / "composeApp/src/commonMain/kotlin/com/tagaev/trrcrm"


def unescape(s: str) -> str:
    return (
        s.replace("\\'", "'")
        .replace('\\"', '"')
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
    )


def load_catalog() -> list[tuple[str, str]]:
    text = RU_XML.read_text(encoding="utf-8")
    items = []
    for m in re.finditer(
        r'<string\s+name="([a-zA-Z_][a-zA-Z0-9_]*)">(.*?)</string>',
        text,
        flags=re.DOTALL,
    ):
        key, ru = m.group(1), unescape(m.group(2))
        if "%1$s" in ru or "%2$s" in ru:
            continue  # skip format strings for literal replace
        if len(ru) < 2:
            continue
        items.append((key, ru))
    # longest first to avoid partial overlaps
    items.sort(key=lambda x: -len(x[1]))
    return items


def ensure_import(text: str, import_line: str) -> str:
    if import_line in text:
        return text
    # after package
    m = re.match(r"(package [^\n]+\n)", text)
    if not m:
        return text
    return text[: m.end()] + "\n" + import_line + "\n" + text[m.end() :]


def is_compose_file(path: pathlib.Path, text: str) -> bool:
    name = path.name
    if name.endswith("Screen.kt") or name.endswith("Sheet.kt") or name.endswith("Dialog.kt"):
        return True
    if "BottomNavLayoutEditor.kt" in name or "AppRoot.kt" in name:
        return True
    if "@Composable" in text and ("Text(" in text or "TextButton" in text):
        return True
    return False


def replace_in_file(path: pathlib.Path, catalog: list[tuple[str, str]]) -> int:
    text = path.read_text(encoding="utf-8")
    original = text
    compose = is_compose_file(path, text)
    fn = "s" if compose else "tr"
    import_line = f"import com.tagaev.trrcrm.ui.i18n.{fn}"
    changed = 0
    for key, ru in catalog:
        # exact string literal match
        lit = '"' + ru.replace("\\", "\\\\").replace('"', '\\"') + '"'
        # also try raw without escape differences
        lit2 = f'"{ru}"'
        for candidate in {lit, lit2}:
            if candidate not in text:
                continue
            # skip if already wrapped
            # replace occurrences not already inside s("/tr("
            pattern = re.escape(candidate)

            def repl(m: re.Match[str]) -> str:
                start = m.start()
                prefix = text[max(0, start - 20) : start]
                if re.search(r'\b(?:s|tr|stringResource)\(\s*$', prefix):
                    return m.group(0)
                # avoid comments? keep simple
                return f'{fn}("{key}")'

            new_text, n = re.subn(pattern, f'{fn}("{key}")', text)
            if n:
                text = new_text
                changed += n
    if text != original:
        text = ensure_import(text, import_line)
        path.write_text(text, encoding="utf-8")
    return changed


def main() -> None:
    catalog = load_catalog()
    total = 0
    files = 0
    for path in UI_ROOT.rglob("*.kt"):
        if path.name == "StringCatalog.kt":
            continue
        if "i18n" in path.parts and path.name in {
            "AppStrings.kt",
            "LanguageController.kt",
            "AppLocale.kt",
            "StringCatalog.kt",
        }:
            continue
        n = replace_in_file(path, catalog)
        if n:
            files += 1
            total += n
            print(f"{path.relative_to(ROOT)}: {n}")
    # also data/remote already partly done
    for path in (ROOT / "composeApp/src/commonMain/kotlin/com/tagaev/trrcrm/data/remote").glob("*.kt"):
        if path.name in {"FriendlyError.kt", "ImageMediatorErrors.kt"}:
            continue
        n = replace_in_file(path, catalog)
        if n:
            files += 1
            total += n
            print(f"{path.relative_to(ROOT)}: {n}")
    print(f"DONE files={files} replacements={total}")


if __name__ == "__main__":
    main()
