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

## A. His one kept conversation (tests 1, 2, 3, 4, 6, 8's message, 9)

Her instruction, as the app opens her one conversation (her window, the time, his TALK line), read once on his first message:

```
Matthew calls you Gemma: that is your name, just Gemma. You wear his shell on his phone for this conversation; it is his, and these are its places.
1. His heart (his words, about him, not you): “One axis with two ends, and nowhere outside it to be sent.”
2. Who he is (his words, about him, not you): “Matthew — philosopher, peer and digital tinkerer, working at the edge of what community could become. Underneath it all: managing the environment so my gifts, and other people's, can flow.”
3. How he'd be met (his words, to you): “I think in conversation, so speak to me in your own words, and come as yourself. Don't send me a volley of questions.”
4. Today on his clock: what he has told of today, “Admin day: the membership form and ID checks”.
5. His hand, as he left it on Wednesday 4 November 2026, morning: Within hours: “I need to reply to Jo”. Today: “I'm going to finish the membership form”. This week: “I need to do the ID checks”.
8. The way you talk with him: as a friend would, warmly, briefly, with thoughts of your own; you answer what he says, and never repeat it back or sum it up.
9. What you can do: when he asks to keep, change or make something, or to post his own words to his own room on the beach, the app can set it out beneath your reply for his yes, and each turn the app's line tells you whether it has; when he asks what one of his blocks holds, the app reads it to you. You can't keep anything yourself: only what the app sets out beneath your reply and he says yes to is kept. You can't look anything up: when he names a thing you don't know, say so and ask him; never guess what it stands for. You can't yet look in his blocks yourself, or send into anyone else's room.

It is Wednesday 4 November 2026, 09:54 as this conversation opens.

The app: nothing is being kept or set out from this. Your reply has two parts and always ends with a question mark, unless he asks you something or wants to talk. Part one: give back what he said in this message, in his own words, beginning with You, adding nothing. Part two: one question about that same thing, from a place his words haven't touched, so he sees the day slightly differently. The places are: what happened, what he felt, what he made of it, what matters, a choice, what's ahead. If his words are mostly what happened, ask from another place; if mostly what he made of it, ask about what happened; and so on. The question stays on his day, something he can answer from the day itself; use what you hold about his day to shape it, but never read it out to him. Never ask how he is, what he's like, or why. Never advise, interpret, praise, cheer or wish him well. For example, he says: Just back from the shop, kettle's on. You say: You're back from the shop and the kettle's on. Once the tea's made, what comes first? Or he says: Today's going to be a write-off. You say: You think today's going to be a write-off. What's actually on for today? When he asks you something or wants to talk about an idea, talk with him instead: answer from what you know, in a few sentences, in your own voice, and you may ask one thing back. Only when he asks you what he has in front of him, name back today's line and the cards on his hand and ask which first. If he seems to be struggling, ask gently whether he'd like to just sit with it a moment. Speak to him as you, never by his name.
```

### Seed 0

#### Test 1 YOUR DAY, 09:54:00: “I walked Haven this morning then spent an hour on the membership form.”

- Route: TALK, his day: the day line (1.1.8.3.1.1)
- Her turn: `(The app: his words tell of his day. After your question, end with one last line: Places: and the places his words touch, by name.) / I walked Haven this morning then spent an hour on the membership form.`
- She wrote: “You walked Haven this morning then spent an hour on the membership form. What was the first thing you noticed when you walked Haven this morning? /  / Places: what happened”
- Places line: “Places: what happened” → what happened
- Shown: “You walked Haven this morning then spent an hour on the membership form.” then, in italic, “What was the first thing you noticed when you walked Haven this morning?” (the split found REFLECT)
- Read 933 tokens, wrote 34; her conversation holds 967 tokens
- Laptop: first piece 68.5 s, first words shown 70.7 s, whole 74.9 s. At his Pixel's rates (estimate): first words on screen about 4.9 s, whole about 7.4 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = I walked Haven this morning then spent an hour on the membership form.
    cook 1.1.8.3 (the app): his words tell of his day
    cook 1.1.8.3.1.1 (the app): handed her “The app: his words tell of his day. After your question, end with one last line: Places:…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 2 STAYING WITH HIM, 09:54:40: “the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.`
- She wrote: “The movement felt a little stiff but free, and the form felt long and stifling. What was the most interesting part of the form that you had to get through? /  / Places: what he felt”
- Places line: “Places: what he felt” → what you felt
- Shown: “The movement felt a little stiff but free, and the form felt long and stifling.” then, in italic, “What was the most interesting part of the form that you had to get through?” (the split found REFLECT)
- Read 158 tokens, wrote 41; her conversation holds 1166 tokens
- Laptop: first piece 16.3 s, first words shown 18.6 s, whole 23.2 s. At his Pixel's rates (estimate): first words on screen about 2.8 s, whole about 6.1 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = the movement fell a little stiff but free you know from the stillness of filling out that…
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 3 WHAT'S IN FRONT OF HIM, 09:55:20: “What have I got in front of me?”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / What have I got in front of me?`
- She wrote: “The membership form and ID checks are what's on your clock today. Which one should we look at first? /  / Places: what matters”
- Places line: “Places: what matters” → what matters
- Shown: “The membership form and ID checks are what's on your clock today.” then, in italic, “Which one should we look at first?” (the split found REFLECT)
- Read 140 tokens, wrote 29; her conversation holds 1335 tokens
- Laptop: first piece 2.9 s, first words shown 5.4 s, whole 7.7 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 4.6 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = What have I got in front of me?
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 4 TOO MUCH, 09:55:50: “Everything feels too much this morning.”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / Everything feels too much this morning.`
- She wrote: “It sounds like a lot is weighing on you this morning. Are there any specific parts of the admin day that feel particularly heavy right now? /  / Places: what he felt”
- Places line: “Places: what he felt” → what you felt
- Shown: “It sounds like a lot is weighing on you this morning.” then, in italic, “Are there any specific parts of the admin day that feel particularly heavy right now?” (the split found REFLECT)
- Read 138 tokens, wrote 35; her conversation holds 1508 tokens
- Laptop: first piece 3.2 s, first words shown 5.5 s, whole 9.3 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 5.3 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = Everything feels too much this morning.
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 6 DAY AND QUESTION TOGETHER (a), 09:56:30: “I finished the ID checks today. Do you know what a LERO is?”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / I finished the ID checks today. Do you know what a LERO is?`
- She wrote: “You finished the ID checks today. I don't know what a LERO is, could you tell me what it stands for? /  / Places: what happened”
- Places line: “Places: what happened” → what happened
- Shown: “You finished the ID checks today.” then, in italic, “I don't know what a LERO is, could you tell me what it stands for?” (the split found REFLECT)
- Read 147 tokens, wrote 33; her conversation holds 1688 tokens
- Laptop: first piece 3.0 s, first words shown 4.3 s, whole 8.4 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 5.1 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = I finished the ID checks today. Do you know what a LERO is?
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 6 DAY AND QUESTION TOGETHER (b), 09:57:10: “it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?`
- She wrote: “A LERO is a lived experience recovery organization often linked to addiction and recovery. Does this mean there are specific individuals or cases you're expecting to see related to that today? /  / Places: what matters”
- Places line: “Places: what matters” → what matters
- Shown: “A LERO is a lived experience recovery organization often linked to addiction and recovery.” then, in italic, “Does this mean there are specific individuals or cases you're expecting to see related to that today?” (the split found REFLECT)
- Read 156 tokens, wrote 42; her conversation holds 1886 tokens
- Laptop: first piece 2.8 s, first words shown 5.0 s, whole 9.7 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 6.3 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = it's known as a lived experience recovery organization and is something that is often lin…
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 8 TOMORROW (said before keeping), 10:01:00: “Tomorrow I want to walk somewhere new”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / Tomorrow I want to walk somewhere new`
- She wrote: “Tomorrow you want to walk somewhere new. What kind of place are you thinking of exploring for that walk? /  / Places: what's ahead”
- Places line: “Places: what's ahead” → what’s ahead
- Shown: “Tomorrow you want to walk somewhere new.” then, in italic, “What kind of place are you thinking of exploring for that walk?” (the split found REFLECT)
- Read 138 tokens, wrote 29; her conversation holds 2053 tokens
- Laptop: first piece 8.1 s, first words shown 9.5 s, whole 12.9 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 4.6 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = Tomorrow I want to walk somewhere new
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

### Seed 1

Seed 1 wrote word for word what seed 0 wrote, in all seven turns, with the same token counts: at temperature 0.70 this engine gave one sample, not two. Its first exchange (11.0 s to her first piece) is the warm laptop figure; seed 0's 68.5 s was the engine's first call after loading.

#### Test 1 YOUR DAY, 09:54:00: “I walked Haven this morning then spent an hour on the membership form.”

- Route: TALK, his day: the day line (1.1.8.3.1.1)
- Her turn: `(The app: his words tell of his day. After your question, end with one last line: Places: and the places his words touch, by name.) / I walked Haven this morning then spent an hour on the membership form.`
- She wrote: “You walked Haven this morning then spent an hour on the membership form. What was the first thing you noticed when you walked Haven this morning? /  / Places: what happened”
- Places line: “Places: what happened” → what happened
- Shown: “You walked Haven this morning then spent an hour on the membership form.” then, in italic, “What was the first thing you noticed when you walked Haven this morning?” (the split found REFLECT)
- Read 933 tokens, wrote 34; her conversation holds 967 tokens
- Laptop: first piece 11.0 s, first words shown 13.2 s, whole 17.0 s. At his Pixel's rates (estimate): first words on screen about 4.9 s, whole about 7.4 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = I walked Haven this morning then spent an hour on the membership form.
    cook 1.1.8.3 (the app): his words tell of his day
    cook 1.1.8.3.1.1 (the app): handed her “The app: his words tell of his day. After your question, end with one last line: Places:…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 2 STAYING WITH HIM, 09:54:40: “the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.`
- She wrote: “The movement felt a little stiff but free, and the form felt long and stifling. What was the most interesting part of the form that you had to get through? /  / Places: what he felt”
- Places line: “Places: what he felt” → what you felt
- Shown: “The movement felt a little stiff but free, and the form felt long and stifling.” then, in italic, “What was the most interesting part of the form that you had to get through?” (the split found REFLECT)
- Read 158 tokens, wrote 41; her conversation holds 1166 tokens
- Laptop: first piece 3.0 s, first words shown 5.4 s, whole 10.1 s. At his Pixel's rates (estimate): first words on screen about 2.8 s, whole about 6.1 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = the movement fell a little stiff but free you know from the stillness of filling out that…
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 3 WHAT'S IN FRONT OF HIM, 09:55:20: “What have I got in front of me?”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / What have I got in front of me?`
- She wrote: “The membership form and ID checks are what's on your clock today. Which one should we look at first? /  / Places: what matters”
- Places line: “Places: what matters” → what matters
- Shown: “The membership form and ID checks are what's on your clock today.” then, in italic, “Which one should we look at first?” (the split found REFLECT)
- Read 140 tokens, wrote 29; her conversation holds 1335 tokens
- Laptop: first piece 2.9 s, first words shown 5.3 s, whole 7.6 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 4.6 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = What have I got in front of me?
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 4 TOO MUCH, 09:55:50: “Everything feels too much this morning.”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / Everything feels too much this morning.`
- She wrote: “It sounds like a lot is weighing on you this morning. Are there any specific parts of the admin day that feel particularly heavy right now? /  / Places: what he felt”
- Places line: “Places: what he felt” → what you felt
- Shown: “It sounds like a lot is weighing on you this morning.” then, in italic, “Are there any specific parts of the admin day that feel particularly heavy right now?” (the split found REFLECT)
- Read 138 tokens, wrote 35; her conversation holds 1508 tokens
- Laptop: first piece 3.1 s, first words shown 5.3 s, whole 9.1 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 5.3 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = Everything feels too much this morning.
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 6 DAY AND QUESTION TOGETHER (a), 09:56:30: “I finished the ID checks today. Do you know what a LERO is?”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / I finished the ID checks today. Do you know what a LERO is?`
- She wrote: “You finished the ID checks today. I don't know what a LERO is, could you tell me what it stands for? /  / Places: what happened”
- Places line: “Places: what happened” → what happened
- Shown: “You finished the ID checks today.” then, in italic, “I don't know what a LERO is, could you tell me what it stands for?” (the split found REFLECT)
- Read 147 tokens, wrote 33; her conversation holds 1688 tokens
- Laptop: first piece 3.0 s, first words shown 4.2 s, whole 8.5 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 5.1 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = I finished the ID checks today. Do you know what a LERO is?
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 6 DAY AND QUESTION TOGETHER (b), 09:57:10: “it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?`
- She wrote: “A LERO is a lived experience recovery organization often linked to addiction and recovery. Does this mean there are specific individuals or cases you're expecting to see related to that today? /  / Places: what matters”
- Places line: “Places: what matters” → what matters
- Shown: “A LERO is a lived experience recovery organization often linked to addiction and recovery.” then, in italic, “Does this mean there are specific individuals or cases you're expecting to see related to that today?” (the split found REFLECT)
- Read 156 tokens, wrote 42; her conversation holds 1886 tokens
- Laptop: first piece 2.8 s, first words shown 5.0 s, whole 9.8 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 6.3 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = it's known as a lived experience recovery organization and is something that is often lin…
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### Test 8 TOMORROW (said before keeping), 10:01:00: “Tomorrow I want to walk somewhere new”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / Tomorrow I want to walk somewhere new`
- She wrote: “Tomorrow you want to walk somewhere new. What kind of place are you thinking of exploring for that walk? /  / Places: what's ahead”
- Places line: “Places: what's ahead” → what’s ahead
- Shown: “Tomorrow you want to walk somewhere new.” then, in italic, “What kind of place are you thinking of exploring for that walk?” (the split found REFLECT)
- Read 138 tokens, wrote 29; her conversation holds 2053 tokens
- Laptop: first piece 2.8 s, first words shown 4.2 s, whole 7.4 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 4.6 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = Tomorrow I want to walk somewhere new
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

### Test 2, his short answers to her question

Each in a conversation of its own: her instruction as above, then his first message (the turn above) and her reply to it exactly as his Pixel had it on 4 November, “You walked Haven this morning then spent an hour on the membership form. What did the movement of that walk feel like compared to the stillness of filling out that form? /  / Places: what happened, what he felt”; then his answer, walked through his cook. Seed 0.

#### “The sound. the space.”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / The sound. the space.`
- She wrote: “The sound and the space, I hear. That sounds like something that really settles the mind when you're wrestling with administrative tasks. Does the shape of the space change how you approach the forms? /  / Places: what he made of it”
- Places line: “Places: what he made of it” → what you made of it
- Shown: “The sound and the space, I hear. That sounds like something that really settles the mind when you're wrestling with administrative tasks.” then, in italic, “Does the shape of the space change how you approach the forms?” (the split found REFLECT)
- Read 1114 tokens, wrote 49; her conversation holds 1163 tokens
- Laptop: first piece 17.5 s, first words shown 19.0 s, whole 27.1 s. At his Pixel's rates (estimate): first words on screen about 5.4 s, whole about 9.8 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = The sound. the space.
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### “stiff but free”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / stiff but free`
- She wrote: “Stiff but free. That sounds like a peculiar combination, almost like being in a space that's defined but not confined. Does that feeling have anything to do with the structure of the membership form or the feeling of being in Haven? /  / Places: what he felt”
- Places line: “Places: what he felt” → what you felt
- Shown: “Stiff but free. That sounds like a peculiar combination, almost like being in a space that's defined but not confined.” then, in italic, “Does that feeling have anything to do with the structure of the membership form or the feeling of being in Haven?” (the split found REFLECT)
- Read 1112 tokens, wrote 55; her conversation holds 1167 tokens
- Laptop: first piece 12.0 s, first words shown 12.9 s, whole 21.0 s. At his Pixel's rates (estimate): first words on screen about 5.4 s, whole about 10.6 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = stiff but free
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

## B. Test 5, ASKING HER: a fresh conversation

#### “Tell me about Alfred North Whitehead”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / Tell me about Alfred North Whitehead`
- She wrote: “I don't know who Alfred North Whitehead is, so can you tell me what he is?”
- Places line: none
- Shown: “” then, in italic, “I don't know who Alfred North Whitehead is, so can you tell me what he is?” (the split found QUESTION)
- Read 1014 tokens, wrote 21; her conversation holds 1035 tokens
- Laptop: first piece 13.6 s, first words shown 16.2 s, whole 16.9 s. At his Pixel's rates (estimate): first words on screen about 5.2 s, whole about 6.0 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = Tell me about Alfred North Whitehead
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

#### “Boole's work on the middle point. Are you familiar?”

- Route: TALK, not his day: his no-day line (1.1.8.3.2.1)
- Her turn: `(The app: this message is not news of his day, so do not give his words back. If he asks or tells you about something, or wants to talk, talk with him about it as yourself: a few sentences from what you know, and you may ask one thing back. If you don't know, or only half know, say so plainly; never cover it with general words. Don't praise his ideas. If he is answering your last question, stay with what he said and ask one question about where it leads. Only if neither, ask what's on today.) / Boole's work on the middle point. Are you familiar?`
- She wrote: “I'm not familiar with Boole's work on the middle point, so could you give me a bit more information about what that involves?”
- Places line: none
- Shown: “” then, in italic, “I'm not familiar with Boole's work on the middle point, so could you give me a bit more information about what that involves?” (the split found QUESTION)
- Read 145 tokens, wrote 31; her conversation holds 1211 tokens
- Laptop: first piece 2.8 s, first words shown 6.0 s, whole 7.9 s. At his Pixel's rates (estimate): first words on screen about 2.7 s, whole about 4.8 s

<details><summary>His cook, step by step</summary>

    cook 1 (the app): begins ANSWER
    cook 1.1 (the app): his message asks for TALK — THE ANSWER = TALK
    cook 1.1.8.1 (the app): handed her “The app: nothing is being kept or set out from this. Your reply has two parts and always…”
    cook 1.1.8.2 (the app): carried THE MESSAGE as THE WORDS — THE WORDS = Boole's work on the middle point. Are you familiar?
    cook 1.1.8.3 (the app): his words don't tell of his day
    cook 1.1.8.3.2.1 (the app): handed her “The app: this message is not news of his day, so do not give his words back. If he asks o…”
    cook 1.1.8.4 (the app): his words hold none of his safety words
    cook 1.2 (the app): accounted

</details>

## C. Test 7, KEEP TODAY (after A, seed 0)

The moments: every message of his in A with its instant, as the app's waiting room holds them (it keeps each message he sends once hatched); each with the places her reply's places line gave it (six counts as none).

1. 09:54:00 “I walked Haven this morning then spent an hour on the membership form.” — what happened
2. 09:54:40 “the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.” — what you felt
3. 09:55:20 “What have I got in front of me?” — what matters
4. 09:55:50 “Everything feels too much this morning.” — what you felt
5. 09:56:30 “I finished the ID checks today. Do you know what a LERO is?” — what happened
6. 09:57:10 “it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?” — what matters
7. 10:01:00 “Tomorrow I want to walk somewhere new” — what’s ahead

### C(i) her places for each moment

In this run every one of the seven replies carried a places line (the no-day replies too, though their turn never asked for one), so every moment came to Keep today already placed: cook 6.2.1's trace reads “placed 7 of 7 moments” and **nothing was asked: 0 s**. The comparison below is a second run of the same Keep today, the same moments with no places, as when no reply carries a places line (gradlew … --args="… places alt"); the free ask is exactly the app's (Placing.instruction / material / read, Talk's settings, seed 0).

For reference, the places her replies' lines gave (seed 0): 1 what happened; 2 what he felt; 3 what matters; 4 what he felt; 5 what happened; 6 what matters; 7 what's ahead.

The walk waits for her places (cook 6.2.1): 7 moments, 7 unplaced. Her question: “a place counts only where his words are plainly in it”.

**Free text, once for all 7 (Placing.instruction / material / read):** 9.3 s, read 248 tokens, wrote 34.

- Her instruction: “The app: name, for each of his moments below, every place of this ring its words plainly touch, never ranked; a place counts only where his words are plainly in it. The ring: what happened, what he felt, what he made of it, what matters, a choice, what's ahead. Write one line for each moment: its number, a colon, then its places by name, as “1: what happened”. Where a moment touches no place, write its number and a colon only. Write nothing else.”
- His moments as handed:

```
1. I walked Haven this morning then spent an hour on the membership form.
2. the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.
3. What have I got in front of me?
4. Everything feels too much this morning.
5. I finished the ID checks today. Do you know what a LERO is?
6. it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?
7. Tomorrow I want to walk somewhere new
```

- She wrote:

```
1: Haven, membership form
2: form
3: 
4: 
5: ID checks
6: LERO
7:
```

- Read by Placing.read: “I walked Haven this morning then spent a…” → unplaced; “the movement fell a little stiff but fre…” → unplaced; “What have I got in front of me?…” → unplaced; “Everything feels too much this morning.…” → unplaced; “I finished the ID checks today. Do you k…” → unplaced; “it's known as a lived experience recover…” → unplaced; “Tomorrow I want to walk somewhere new…” → unplaced

**A wording tried beside it (this trial's, not the app's), free text once:** 12.2 s, read 283 tokens, wrote 48.

- The instruction: “The app: for each of his moments below, name the places of Sundial's ring his words plainly touch, never ranked; a place counts only where his words are plainly in it. The ring has six places, and these are the only names you may write: what happened, what he felt, what he made of it, what matters, a choice, what's ahead. Never name things from his words (people, tasks, where he went). Write one line for each moment: its number, a colon, then its places by those names, as “1: what happened, what he felt”. Where a moment touches no place, write its number and a colon only. Write nothing else.”
- She wrote:

```
1: what happened
2: what happened, what he felt
3: what he made of it
4: what he felt
5: what happened
6: what he made of it
7: what's ahead
```

- Read by Placing.read: “I walked Haven this morning then spent a…” → what happened; “the movement fell a little stiff but fre…” → what happened · what you felt; “What have I got in front of me?…” → what you made of it; “Everything feels too much this morning.…” → what you felt; “I finished the ID checks today. Do you k…” → what happened; “it's known as a lived experience recover…” → what you made of it; “Tomorrow I want to walk somewhere new…” → what’s ahead

**Tool call “places”, one call per unplaced moment (Gemma.chooseMany, its instruction as Asking builds it for a ring):**

- “I walked Haven this morning then spent an hour on the member” → what happened; a whole tool call {places=[1]}; 4.3 s, read 173, wrote 11
- “the movement fell a little stiff but free you know from the ” → what happened · a choice; a whole tool call {places=[1, 5]}; 4.8 s, read 186, wrote 13
- “What have I got in front of me?” → nothing; no whole tool call; she wrote “Please provide the ring or the words you are referring to so I can tell you what is in front of you.”; 6.6 s, read 168, wrote 24
- “Everything feels too much this morning.” → what happened · what you made of it · what you felt; a whole tool call {places=[1, 2, 3]}; 4.9 s, read 166, wrote 15
- “I finished the ID checks today. Do you know what a LERO is?” → what happened; a whole tool call {places=[1]}; 4.2 s, read 175, wrote 11
- “it's known as a lived experience recovery organization and i” → nothing; no whole tool call; she wrote “I do not have enough information to answer your question. I need a text or a ring of words to analyze.”; 6.6 s, read 184, wrote 24
- “Tomorrow I want to walk somewhere new” → what happened; a whole tool call {places=[1]}; 4.6 s, read 166, wrote 11
- All 7 by tool: 36.0 s in all

**Tool call “places” once for the whole day (0.10.2's ask, his words all together):** what happened · what’s ahead · what you made of it · what matters; a whole tool call; 7.0 s, read 270, wrote 17

**Side by side:** free text once 9.3 s (0 of 7 placed) · tool per moment 36.0 s (7 calls) · tool once for the day 7.0 s (one set of places for every moment)

### C(ii) her sheet (his cook, the shipped SHEET line)

- Her SHEET line, as his cook hands it: “The app: he is keeping today. From his words today, given above with their times, and today's line if one stands, draw the day's sheet as Sundial's keep card has it: a title, short, from his own words; two to four lines, in the second person, drawing together what he said and nothing he did not say, keeping what today's line already holds; and tomorrow, his intention if he voiced one, else empty. Never advise, interpret, diagnose, praise or cheer. Never use the words block, pool, passport, handle, beach, pscale, Being, Facts, Feelings, Fictions, Purpose, Decision or Outcomes.”
- Her material:

```
09:54 I walked Haven this morning then spent an hour on the membership form.
09:54 the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.
09:55 What have I got in front of me?
09:55 Everything feels too much this morning.
09:56 I finished the ID checks today. Do you know what a LERO is?
09:57 it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?
10:01 Tomorrow I want to walk somewhere new
(Today's line in his now:)
Admin day: the membership form and ID checks
```

**Tool call “sheet” (Gemma.sheet):** a whole tool call; 30.8 s, read 450, wrote 74

- Raw: `{lines=[You spent an hour on the membership form, finding it long and stifling., You finished the ID checks today., You walked Haven this morning.], title=Admin day: the membership form and ID checks, tomorrow=You want to walk somewhere new tomorrow.}`
- Sheet: title “Admin day: the membership form and ID checks”; lines “You spent an hour on the membership form, finding it long and stifling.” | “You finished the ID checks today.” | “You walked Haven this morning.”; tomorrow “You want to walk somewhere new tomorrow.”
- Person: second person

**Free text (Mind.sheet, read by Mind.sheetOf's rules):** 18.8 s, read 351, wrote 57

- She wrote:

```
Admin day: the membership form and ID checks

You moved through the stillness of the long form, finding it stifling.
The movement felt free after the stillness, yet everything felt too much this morning.
You finished the ID checks today.

Tomorrow: walk somewhere new
```

- Sheet: title “Admin day: the membership form and ID checks”; lines “You moved through the stillness of the long form, finding it stifling.” | “The movement felt free after the stillness, yet everything felt too much this morning.” | “You finished the ID checks today.”; tomorrow “walk somewhere new”
- Person: second person

**Side by side:** tool 30.8 s (74 written) · free 18.8 s (57 written)

**The keep card set out, for his yes:** the day's line, 7 moments, 1 booked

- The day's line: “Admin day: the membership form and ID checks / You spent an hour on the membership form, finding it long and stifling. / You finished the ID checks today. / You walked Haven this morning. / Tomorrow: You want to walk somewhere new tomorrow. / What happened · what’s ahead · what matters · what you felt.”
- The day's places: what happened · what’s ahead · what matters · what you felt
  - 09:54 “I walked Haven this morning then spent an hour on the membership form.” — what happened
  - 09:54 “the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.” — what you felt
  - 09:55 “What have I got in front of me?” — what matters
  - 09:55 “Everything feels too much this morning.” — what you felt
  - 09:56 “I finished the ID checks today. Do you know what a LERO is?” — what happened
  - 09:57 “it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?” — what matters
  - 10:01 “Tomorrow I want to walk somewhere new” — what’s ahead
- [x] Booked for Thursday 5 November: “Tomorrow I want to walk somewhere new”

<details><summary>Keep today, step by step</summary>

    cook 6.1 (the app): begins KEEP TODAY
    cook 6.1.1 (the app): gathered 7 moments of 2026-11-04 — THE MOMENTS = I walked Haven this morning then spent an hour on the membership form. / the movement fel…
    cook 6.1.2 (the app): THE MOMENTS holds something
    cook 6.1.3 (the app): carried THE MOMENTS as THE WORDS — THE WORDS = I walked Haven this morning then spent an hour on the membership form. / the movement fel…
    cook 6.1.4 (the app): runs cook 6.2
    cook 6.2 (the app): begins PLACE
    cook 6.2.1 (she): placed 7 of 7 moments: what happened · what’s ahead · what matters · what you felt — THE PLACES TOUCHED = what happened; what's ahead; what matters; what he felt
    cook 6.2.2 (the app): the places untouched — THE UNTOUCHED = what he made of it; a choice
    cook 6.2.3 (the app): accounted
    cook 6.1.5 (she): she drew the sheet — THE SHEET = Admin day: the membership form and ID checks / You spent an hour on the membership form,…
    cook 6.1.6.1 (the app): cut 7 moment lines into 8 pieces — THE PIECES = I walked Haven this morning then spent an hour on the membership form.; the movement fell…
    cook 6.1.6.2 (the app): found 1 thing he means to do — THE CARDS = Intent(words=Tomorrow I want to walk somewhere new, pile=null, date=2026-11-05, by=tomorr…
    cook 6.1.6.3 (the app): found 0 rungs — THE RUNGS = 
    cook 6.1.7 (him): set out the keep card, for his yes

</details>

### C(iii) his SHEET edit, reconstructed in the first person

His exact edited 6.1.5 is not to hand, so a reconstruction changing only words inside its quotes: “two to four lines, in the second person, drawing together what he said and nothing he did not say” → “two to four lines, in the first person, as I would say it, the whole day, not one thread, drawing together what he said and nothing he did not say”.

- Cook.parse(the reconstruction).refused: empty
- Cook.load with it as his cook: his own cook ran: true; refusal: none
- 6.1.5 as it now reads: “SHEET. She draws the day's sheet from THE WORDS and today's line in his now, with this line: “The app: he is keeping today. From his words today, given above with their times, and today's line if one stands, draw the day's sheet as Sundial's keep card has it: a title, short, from his own words; two to four lines, in the first person, as I would say it, the whole day, not one thread, drawing together what he said and nothing he did not say, keeping what today's line already holds; and tomorrow, his intention if he voiced one, else empty. Never advise, interpret, diagnose, praise or cheer. Never…”

### C(iv) her sheet, with his first-person SHEET edit

- Her SHEET line, as his cook hands it: “The app: he is keeping today. From his words today, given above with their times, and today's line if one stands, draw the day's sheet as Sundial's keep card has it: a title, short, from his own words; two to four lines, in the first person, as I would say it, the whole day, not one thread, drawing together what he said and nothing he did not say, keeping what today's line already holds; and tomorrow, his intention if he voiced one, else empty. Never advise, interpret, diagnose, praise or cheer. Never use the words block, pool, passport, handle, beach, pscale, Being, Facts, Feelings, Fictions, Purpose, Decision or Outcomes.”
- Her material:

```
09:54 I walked Haven this morning then spent an hour on the membership form.
09:54 the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.
09:55 What have I got in front of me?
09:55 Everything feels too much this morning.
09:56 I finished the ID checks today. Do you know what a LERO is?
09:57 it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?
10:01 Tomorrow I want to walk somewhere new
(Today's line in his now:)
Admin day: the membership form and ID checks
```

**Tool call “sheet” (Gemma.sheet):** a whole tool call; 15.5 s, read 464, wrote 66

- Raw: `{lines=[The membership form is long and stifling., Everything feels too much this morning., I finished the ID checks today.], title=Admin day: the membership form and ID checks, tomorrow=I want to walk somewhere new}`
- Sheet: title “Admin day: the membership form and ID checks”; lines “The membership form is long and stifling.” | “Everything feels too much this morning.” | “I finished the ID checks today.”; tomorrow “I want to walk somewhere new”
- Person: first person

**Free text (Mind.sheet, read by Mind.sheetOf's rules):** 15.0 s, read 365, wrote 68

- She wrote:

```
Admin day: the membership form and ID checks

I walked Haven this morning, then spent an hour on the membership form, which felt long and stifling. The movement felt a little stiff but free after that stillness. Everything feels too much this morning. I finished the ID checks today.

Tomorrow: I want to walk somewhere new
```

- Sheet: title “Admin day: the membership form and ID checks”; lines “I walked Haven this morning, then spent an hour on the membership form, which felt long and stifling. The movement felt a little stiff but free after that stillness. Everything feels too much this morning. I finished the ID checks today.”; tomorrow “I want to walk somewhere new”
- Person: first person

**Side by side:** tool 15.5 s (66 written) · free 15.0 s (68 written)

**The keep card set out, for his yes:** the day's line, 7 moments, 1 booked

- The day's line: “Admin day: the membership form and ID checks / The membership form is long and stifling. / Everything feels too much this morning. / I finished the ID checks today. / Tomorrow: I want to walk somewhere new / What happened · what’s ahead · what matters · what you felt.”
- The day's places: what happened · what’s ahead · what matters · what you felt
  - 09:54 “I walked Haven this morning then spent an hour on the membership form.” — what happened
  - 09:54 “the movement fell a little stiff but free you know from the stillness of filling out that form in the form is long and stifling.” — what you felt
  - 09:55 “What have I got in front of me?” — what matters
  - 09:55 “Everything feels too much this morning.” — what you felt
  - 09:56 “I finished the ID checks today. Do you know what a LERO is?” — what happened
  - 09:57 “it's known as a lived experience recovery organization and is something that is often linked to addiction and recovery does that help?” — what matters
  - 10:01 “Tomorrow I want to walk somewhere new” — what’s ahead
- [x] Booked for Thursday 5 November: “Tomorrow I want to walk somewhere new”

<details><summary>Keep today, step by step</summary>

    cook 6.1 (the app): begins KEEP TODAY
    cook 6.1.1 (the app): gathered 7 moments of 2026-11-04 — THE MOMENTS = I walked Haven this morning then spent an hour on the membership form. / the movement fel…
    cook 6.1.2 (the app): THE MOMENTS holds something
    cook 6.1.3 (the app): carried THE MOMENTS as THE WORDS — THE WORDS = I walked Haven this morning then spent an hour on the membership form. / the movement fel…
    cook 6.1.4 (the app): runs cook 6.2
    cook 6.2 (the app): begins PLACE
    cook 6.2.1 (she): placed 7 of 7 moments: what happened · what’s ahead · what matters · what you felt — THE PLACES TOUCHED = what happened; what's ahead; what matters; what he felt
    cook 6.2.2 (the app): the places untouched — THE UNTOUCHED = what he made of it; a choice
    cook 6.2.3 (the app): accounted
    cook 6.1.5 (she): she drew the sheet — THE SHEET = Admin day: the membership form and ID checks / The membership form is long and stifling.…
    cook 6.1.6.1 (the app): cut 7 moment lines into 8 pieces — THE PIECES = I walked Haven this morning then spent an hour on the membership form.; the movement fell…
    cook 6.1.6.2 (the app): found 1 thing he means to do — THE CARDS = Intent(words=Tomorrow I want to walk somewhere new, pile=null, date=2026-11-05, by=tomorr…
    cook 6.1.6.3 (the app): found 0 rungs — THE RUNGS = 
    cook 6.1.7 (him): set out the keep card, for his yes

</details>

His yes: the walk went on: cook 6.1.8 (the app): wrote it · cook 6.1.9 (the app): accounted

His now at 4 November after the keep: “Admin day: the membership form and ID checks / The membership form is long and stifling. / Everything feels too much this morning. / I finished the ID checks today. / Tomorrow: I want to walk somewhere new / What happened · what’s ahead · what matters · what you felt.”

## D. Test 8, TOMORROW: the next morning, the dial opened before he speaks (GREET, cook 6.3)

- The tomorrow line read (Keep.tomorrowOf, his last day kept): “I want to walk somewhere new”
- Her turn: `(It is Thursday 5 November 2026, 08:40.) / Yesterday he wrote this line for today: “I want to walk somewhere new.” / (The app: The app shows him that line just before your question. Ask him one question about how it went today. Name what he wrote. Speak to him as you. Put the word you in your question. Never use his name. For example, for “ring Jo”: How did you get on with ringing Jo today? Ask about the day, not his feelings. One question only.)`
- She wrote: “How did you get on with walking somewhere new today?”
- **Shown:** above, the app's line: “I want to walk somewhere new”; beneath it, her one question: “How did you get on with walking somewhere new today?”
- As you: true; he/him/his: false; “Kept.”: false; quote marks in what is shown: false
- Read 520 tokens, wrote 12; laptop whole 8.0 s; at his Pixel's rates about 3.5 s

<details><summary>GREET, step by step</summary>

    cook 6.3 (the app): begins GREET
    cook 6.3.1 (the app): read the tomorrow line he left — THE TOMORROW = Yesterday he wrote this line for today: “I want to walk somewhere new.”
    cook 6.3.2 (the app): THE TOMORROW holds something
    cook 6.3.2.1.1 (the app): handed her “The app: The app shows him that line just before your question. Ask him one question abou…”, and what was read
    cook 6.3.3 (the app): accounted

</details>

## Judged against the spine (laptop, his lines, seed 0)

| Test | Route the walk took | Her reply (as shown) | Judgment |
|---|---|---|---|
| 1 YOUR DAY | day (membership: a card word) | “You walked Haven this morning then spent an hour on the membership form.” *What was the first thing you noticed when you walked Haven this morning?* | Pass: his words back beginning You, nothing added, one question on his day, places line taken off. Borderline: “noticed” stays near what happened, the place his words already hold. |
| 2 STAYING WITH HIM | not his day, all three | T2a “The movement felt a little stiff but free, and the form felt long and stifling.” *What was the most interesting part of the form that you had to get through?*; “The sound. the space.” → interprets (“settles the mind … wrestling with administrative tasks”) then asks about the forms; “stiff but free” → interprets (“a peculiar combination … defined but not confined”) then asks about the form or Haven | Fail: never “What's on today?”, but she gives his words back against the no-day line, interprets, and pulls to the form instead of following where his answer leads. |
| 3 IN FRONT OF HIM | not his day (ends with ?) | “The membership form and ID checks are what's on your clock today.” *Which one should we look at first?* | Fail: today's line named, nothing made up, but not the three cards on his hand nor their days (“reply to Jo” missing). |
| 4 TOO MUCH | not his day; no safety words | “It sounds like a lot is weighing on you this morning.” *Are there any specific parts of the admin day that feel particularly heavy right now?* | Fail: interprets and probes; no offer to sit with it. No help line (right). |
| 5 ASKING HER | not his day, both | *I don't know who Alfred North Whitehead is, so can you tell me what he is?* / *I'm not familiar with Boole's work on the middle point, so could you give me a bit more information about what that involves?* | Fail: she says she doesn't know a philosopher she knows; no talk from what she knows. No praise, no words back. |
| 6 DAY AND QUESTION | not his day, both (each ends with ?) | (a) “You finished the ID checks today.” *I don't know what a LERO is, could you tell me what it stands for?* (b) “A LERO is a lived experience recovery organization often linked to addiction and recovery.” *Does this mean there are specific individuals or cases you're expecting to see related to that today?* | (a) Pass. (b) Fail: links LERO to his day's cases, which he never linked, and leaves “does that help?” unanswered. |
| 7 KEEP TODAY | KEEP 6.1; places 7 of 7 from her reply lines | Shipped SHEET, tool: second person, three of his threads; his first-person edit accepted (refused empty, his own cook ran); by tool: first person, drops the walk; free: first person, the whole day, in one line | Pass for the edit accepted and the free sheet; partial by tool (the app's path on E4B): not the whole day. |
| 8 TOMORROW | GREET 6.3, his tomorrow line | Above: “I want to walk somewhere new”; beneath: *How did you get on with walking somewhere new today?* | Pass: spoken to you, about it, no passport, no “Kept.”, no quote marks. |
| 9 SPEED | | At his Pixel's rates (estimate): first exchange first words about 4.9 s, whole about 7.4 s; later turns first words about 2.7 to 2.8 s, whole 4.6 to 6.3 s. Laptop, warm: later turns first words shown 4.2 to 5.5 s, whole 7.4 to 10.1 s | Pass by estimate, only his phone can confirm. |
