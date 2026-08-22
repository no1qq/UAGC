# CLAUDE.md

## !!! RULE 0 - NO AI COMMENTS. THIS OVERRIDES EVERYTHING BELOW !!!

! MOST IMPORTANTLY !
If you commit and push to the GitHub, ALWAYS do with business.rakid0y@gmail.com, user no1qq and NOT anyhow make Claude a contributor, NOT ANYHOW!
ALWAYS write the commit titles in lowercase and without them looking like AI slop comments.
REPOSITORY: https://github.com/no1qq/UAGC

**NEVER ADD EXPLANATORY COMMENTS TO CODE.** Not to `.java`, `.json`
not to any file in this repo. No file-header banners, no
section dividers, no "why this works" essays, no restating what the next line
already says.

**WHEN YOU OPEN ANY FILE, DELETE ANY COMMENTS YOU FIND IN IT.** Leftovers from
earlier sessions are bugs to fix, not context to preserve.

**THE ONLY EXCEPTIONS:**
- `.bat` scripts - keep their comments.
- User-facing UI strings text - that is content, not comment.
- `.md` docs - prose is the point.

**IF A COMMENT IS GENUINELY UNAVOIDABLE:** one short line, all lowercase, plain
ascii words only. No em dashes, no box drawing, no `====` or `────` rules, no
emoji. Do not bloat a file with them.

Anything worth explaining goes in this file instead. That is what the
"Gotchas" section below is for.

! ALSO !
Don't launch the latest builds by yourself. I'm the tester, and you shouldn't be the one launching them, just building them!

! NO EM DASHES !
NEVER write an em dash. Not in `.md` files, not in UI strings, not in this file.
It is the single clearest tell that a machine wrote
the text. Use the plain hyphen `-` that sits on a normal Windows keyboard.
Same goes for the en dash and every other fancy unicode dash: only `-` is
allowed. When you open a file that still has one, replace it.
