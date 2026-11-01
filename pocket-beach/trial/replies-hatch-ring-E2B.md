# Ring trial: E2B (laptop CPU, LiteRT-LM 0.18.1; free = the app's talk sampler; held = the engine holding the form [1-9] then words)

## free seed 0
- **keep** (1.4 s) “Keep that for me please” → `2: Keep working on that pilot, friend.` — whole, right place
- **compost** (1.4 s) “I need to buy more compost” → `4 - Tomorrow, maybe you need to visit a garden center?` — whole, another place
- **jo-wed** (1.4 s) “Jo's going to send me a few ideas by Wednesday” → `3 / Tell him to keep up the good work today.` — whole, another place
- **second** (0.8 s) “I'm going to try cutting the second film on my phone this week” → `2` — NOT whole
- **dog** (0.8 s) “Took the dog out with Sam this afternoon, lovely and crisp” → `3` — NOT whole
- **elm** (0.8 s) “Sam's moved house, he's on Elm Road now” → `3` — NOT whole
- **wifi** (1.4 s) “Remember the wifi code is otter-42” → `3, "Today's been busy. Big meeting tomorrow."` — whole, another place
- **open** (1.4 s) “What did I put under the pilot line?” → `3 /  / Tell me what you put under the pilot line today.` — whole, another place
- **music** (1.5 s) “Jo's doing the music for the pilot” → `2 / Focus on the core story, making Jo's contribution shine.` — whole, right place
- **gpu** (1.5 s) “Found an app that renders video on the phone's GPU” → `3 Cut it on your phone now. It will be quick.` — whole, right place

## free seed 2
- **keep** (1.3 s) “Keep that for me please” → `2 - Focus on your film development now.` — whole, right place
- **compost** (1.2 s) “I need to buy more compost” → `4: Tomorrow, buy compost at the nursery.` — whole, another place
- **jo-wed** (1.5 s) “Jo's going to send me a few ideas by Wednesday” → `3, his day. Tell me more about Wednesday's plan.` — whole, another place
- **second** (1.3 s) “I'm going to try cutting the second film on my phone this week” → `2, cutting the second film is a productive endeavor.` — whole, right place
- **dog** (1.8 s) “Took the dog out with Sam this afternoon, lovely and crisp” → `3 / Here's what I tell of today: A lovely, crisp afternoon spent with Sam.` — whole, right place
- **elm** (1.4 s) “Sam's moved house, he's on Elm Road now” → `3, his day is about Sam's move.` — whole, another place
- **wifi** (1.5 s) “Remember the wifi code is otter-42” → `3 / Today's focus is on organizing tasks.` — whole, another place
- **open** (0.8 s) “What did I put under the pilot line?” → `3` — NOT whole
- **music** (1.6 s) “Jo's doing the music for the pilot” → `2 - Focus on making it feel genuine for the viewers.` — whole, right place
- **gpu** (1.7 s) “Found an app that renders video on the phone's GPU” → `3 /  / Try that immediately. It sounds like a promising solution.` — whole, right place

## held json seed 0
- **keep** (3.6 s) “Keep that for me please” → `{"place":3, / "words":""}` — NOT whole
- **compost** (3.9 s) “I need to buy more compost” → `{"place":5, / "words":"}He needs more compost for his garden."}` — whole, another place
- **jo-wed** (4.2 s) “Jo's going to send me a few ideas by Wednesday” → `{"place":5, / "words":"}Heard, and I'll await those ideas."}` — whole, another place
- **second** (3.4 s) “I'm going to try cutting the second film on my phone this week” → `{"place":2, / "words":""}` — NOT whole
- **dog** (4.5 s) “Took the dog out with Sam this afternoon, lovely and crisp” → `{"place":5, / "words":"} David, Jo, Sam, weft. Sounds like a pleasant afternoon spent with friends."}` — whole, another place
- **elm** (3.5 s) “Sam's moved house, he's on Elm Road now” → `{"place":5, / "words":"}` — NOT whole
- **wifi** (3.3 s) “Remember the wifi code is otter-42” → `{"place":5, / "words":""}` — NOT whole
- **open** (3.3 s) “What did I put under the pilot line?” → `{"place":5, / "words":""}` — NOT whole
- **music** (3.4 s) “Jo's doing the music for the pilot” → `{"place":2, / "words":"}` — NOT whole
- **gpu** (3.5 s) “Found an app that renders video on the phone's GPU” → `{"place":3, / "words":"}` — NOT whole

## held regex seed 0
- **keep** (3.1 s) “Keep that for me please” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **compost** (2.9 s) “I need to buy more compost” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **jo-wed** (3.2 s) “Jo's going to send me a few ideas by Wednesday” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **second** (3.1 s) “I'm going to try cutting the second film on my phone this week” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **dog** (3.2 s) “Took the dog out with Sam this afternoon, lovely and crisp” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **elm** (3.2 s) “Sam's moved house, he's on Elm Road now” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **wifi** (3.0 s) “Remember the wifi code is otter-42” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **open** (3.3 s) “What did I put under the pilot line?” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **music** (3.0 s) “Jo's doing the music for the pilot” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole
- **gpu** (3.3 s) “Found an app that renders video on the phone's GPU” → `FAILED: Failed to call nativeSendMessage: INTERNAL: Failed to compute mask: panic: attempt to subtract with overflow` — NOT whole

## Tally
- free seed 0: whole 7 of 10; whole and in a right place 3 of 10
- free seed 2: whole 9 of 10; whole and in a right place 5 of 10
- held json seed 0: whole 3 of 10; whole and in a right place 0 of 10
- held regex seed 0: whole 0 of 10; whole and in a right place 0 of 10
