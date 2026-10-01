# Deutsch Dictionary CVUT FS - Project Context

Tento soubor slouží jako referenční dokument o struktuře a funkčnosti projektu. Jeho cílem je usnadnit budoucí úpravy a poskytovat kontext (např. jako vstupní bod pro efektivní promptování AI).

## O projektu (About the Project)
Jedná se o Android aplikaci postavenou na moderním UI toolkitu **Jetpack Compose**. Aplikace slouží pro výuku a procvičování německých slovíček. Je navržena (zřejmě) primárně pro studenty ČVUT FS.

## Architektura a Struktura projektu
Aplikace aktuálně nepoužívá striktní komplexní rozdělení do složité architektury (jako MVVM s Repozitáři), většina aplikační logiky je rozdělena mezi dedikované logické třídy (`Manager`y a `Engine`) a samotné Compose UI.

### Klíčové soubory a struktura (MVVM):

- **`MainActivity.kt`**
  - Minimalistický hlavní vstupní bod aplikace. Pouze spouští navigační graf.

- **`ui/navigation/AppNavigation.kt`**
  - Definuje navigační graf (pomocí `androidx.navigation.compose.NavHost`).
  - Spojuje obrazovky a předává parametry.

- **`ui/screens/`**
  - Složka obsahující jednotlivé UI obrazovky rozdělené podle sekcí (`dictionary`, `mastery`, `practice`, `MainMenuScreen.kt`). Každá obrazovka (`@Composable`) má nyní svůj vlastní soubor pro lepší čitelnost.

- **`ui/screens/practice/PracticeSessionViewModel.kt`**
  - **ViewModel**, který spravuje veškerý stav během tréninku (aktuální otázka, skóre, odpovědi). 
  - Odděluje komplexní UI logiku od samotného vykreslování v `PracticeSessionScreen.kt`.

- **`models.kt` & `ui/models/PracticeModels.kt`**
  - Datové modely (`Lesson`, `Category`, `Word` včetně podpory synonym). `PracticeModels.kt` obsahuje modely spojené čistě s průběhem procvičování (`PracticeConfig`, `SessionResult`, atd.).
  
- **`DictionaryManager.kt`**
  - Zajišťuje iteraci a načítání lekcí ze složky `assets/`.
  - Hledá všechny soubory končící na `.json`, načítá je do objektů přes `kotlinx.serialization` a řadí je chronologicky podle čísel v názvu souboru (např. `lesson1.json`, `lesson2.json` atd.).

- **`Domain.kt`**
  - Obsahuje jádro **doménové (business) logiky**, která je oddělena od Android frameworku.
  - **`AnswerValidator`**: Funkce, která porovnává odpověď uživatele proti hlavnímu slovu i jeho případným synonymům. U němčiny inteligentně ignoruje na začátku napsané členy (der/die/das/ein/eine).
  - **`MasteryUpdater`**: Určuje, jak se změní hodnota "Mastery" (úrovně zvládnutí) u slovíčka na základě správné/špatné odpovědi a formátu otázky (výběr z možností vs. psaní textu, směr překladu).
  - **`PracticeSessionEngine`**: Pokročilý algoritmus pro výběr dalšího slovíčka do testu. Zohledňuje:
    - Dosavadní Mastery slovíčka.
    - Dobu, která uplynula od posledního procvičování (efekt zapomínání).
    - Automaticky přizpůsobuje obtížnost (vybírá od Multiple Choice po Written formát podle úrovně Mastery) pomocí pravděpodobnostní "rulety" s jasně danými pravidly, například formát Written (CZ -> DE) je od 70 % vynucen.

- **`MasteryManager.kt`**
  - Zodpovídá za persistenci (trvalé ukládání) postupu uživatele do interního souborového systému zařízení (`mastery_data.json`).
  - Spravuje načítání a ukládání progressu u jednotlivých slov.
  - Implementuje tzv. "Legacy Key" fallback - mechanismus pro zpětnou kompatibilitu, pokud by stará slovíčka neměla vlastní ID z JSONu, spárují se klíčem z `lessonId` a překladu.

## Jak funguje cyklus procvičování
1. Uživatel v `PracticeSetupScreen` nastaví rozsah (lekce), formáty otázek a počet slov (či nekonečný režim).
2. Tím se spustí `PracticeSessionScreen`, která inicializuje `PracticeSessionEngine` (načte všechna příslušná slova z `DictionaryManager`u).
3. Algoritmus pro každé kolo vybere nejvhodnější slovíčko. Pro formát výběru z možností ("Multiple Choice") vygeneruje náhodné špatné odpovědi.
4. Uživatel odpoví. Přes `MasteryUpdater` se vypočítá nové skóre Mastery. `MasteryManager` to ihned uloží na disk.
5. Po skončení session se data spočítají a v `PracticeResultScreen` se ukáže procentuální úspěšnost a součet přírůstků/úbytků Mastery za každou lekci.

## Tipy pro další rozvoj & Promptování v budoucnu
*Tento oddíl slouží přímo jako vodítko při zadávání dalších požadavků na úpravy.*

- **Úpravy a rozšiřování UI:** Komponenty a obrazovky jsou nyní rozděleny ve složce `ui/screens/`. Navigace je v `AppNavigation.kt`. Pokud chcete novou obrazovku, vytvořte ji v příslušném balíčku a zaregistrujte v navigaci.
- **Logika a body v procvičování:** Pokud chcete změnit to, kolik bodů se odečte/přičte za špatnou odpověď, nasměrujte úpravy do `Domain.kt`. Pokud potřebujete změnit to, co se děje na obrazovce po zodpovězení, upravte `PracticeSessionViewModel.kt`.
- **Přidávání slovníků:** Projekt je dimenzován na to, aby bylo přidání slovíček maximálně jednoduché bez programování. Stačí vytvořit nový `lessonXX.json` s validní strukturou a hodit ho do složky `src/main/assets/`. Systém (`DictionaryManager`) se o něj postará.
