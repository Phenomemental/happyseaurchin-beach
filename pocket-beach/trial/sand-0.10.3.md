# Sundial 0.10.3 on the laptop: the nine tests' recipe parts, on Gemma 4 E4B, with Matthew's lines

Run 2026-11-04T10:56:23.463368583Z, this machine's CPU (LiteRT-LM 0.18.1, Gemma 4 E4B, a context of 8192). His Talk settings as his log of 4 November gives them: temperature 0.70, top-k 64, top-p 0.95, replies up to 1,024 tokens, thinking off; seeds 0, 1 for A, seed 0 elsewhere (the app's own seed is 0).

## His cook

The shipped cook (assets/procedures/cook.json as it stands in the tree) with only the words inside the quotes at 1.1.8.1 (his TALK line, log line 9) and 1.1.8.3.2.1 (his no-day line, log line 61) replaced by his, word for word; written to his pocket as cook:Phenomemental.

- Cook.parse(his cook).refused: empty; unreadable: none
- His 1.1.8.1 now reads: “Hand her this line: “The app: nothing is being kept or set out from this. Your reply has two parts and always ends with a question mark, unl…”
- His 1.1.8.3.2.1 now reads: “Hand her this line: “The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.””
- The day line, 1.1.8.3.1.1, shipped and unchanged: “Hand her this line: “The app: his words tell of his day. After your question, end with one last line: Places: and the places his words touch, by name.””
- The day check, 1.1.8.3, shipped: “Does THE MESSAGE tell of his day: three sentences or more and forty words or more in all, or a word of five letters or more that a card on his hand also holds? Never when it ends with a question mark, or opens with one of: add, note, change, make, post, put, write, keep, remember, save, remove, delete, rename, read, show, tell, do.”

His pocket, set before the run: hatched as Phenomemental, her name Gemma; today's line in his now (a line Sundial wrote earlier today, at 07:54, so Keep today may write over it): “Admin day: the membership form and ID checks”; on his hand (ahead:Phenomemental), one card in each of three piles: within hours: “I need to reply to Jo”; today: “I'm going to finish the membership form”; this week: “I need to do the ID checks”. The world's cards() (the day check's) returns his hand as his ahead holds it.

- Cook.load(his pocket): his own cook ran: true; refusal: none
- Her recipe for talk (Cook.talkRecipe, read once into her instruction) is his TALK line word for word: true
- The world's cards(): {1.1=I need to reply to Jo, 2.1=I'm going to finish the membership form, 3.1=I need to do the ID checks}

