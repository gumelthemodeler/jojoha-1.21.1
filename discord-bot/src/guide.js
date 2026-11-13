export const PAGES = [
  {
    id: 'stands',
    title: 'Stands',
    summary: 'Use a Stand Arrow on yourself',
    intro: 'Use a **Stand Arrow** on yourself to get a Stand. You can only have one Stand.',
    sections: [
      ['Requirements',
        '- **Worthiness 15** or higher\n'
        + '- Below 15, the arrow only damages you'],
      ['Success chance',
        '- **Stand Arrow:** 25% at Worthiness 15, up to 50% at max Worthiness\n'
        + '- **Stand Arrow Shard:** 10% at Worthiness 15, up to about 33% at max Worthiness\n'
        + '- A shard that fails is used up'],
      ['Where to find arrows and shards',
        '- Woodland mansions\n'
        + '- Ancient cities\n'
        + '- Bastions\n'
        + '- End cities\n'
        + '- Strongholds\n'
        + '- Nether fortresses\n'
        + '- Desert and jungle temples\n'
        + '- Dungeons\n'
        + '- Fishing (very rare)'],
    ],
  },
  {
    id: 'hamon',
    title: 'Hamon',
    summary: 'Unlock it in the skill tree or read a book',
    intro: 'Hamon is a fighting style any living player can learn.',
    sections: [
      ['How to learn it',
        '- Unlock **Hamon** in your skill tree, or\n'
        + '- Read a **Basic Hamon Book**\n\n'
        + 'Both give you the Hermit Path.'],
      ['Paths',
        '- There are 8 paths: Hermit, Warrior, Wisdom, Defender, Envious, Trickster, Prideful, Stylish\n'
        + '- Each of the other 7 is learned from its own **Path Book**\n'
        + '- Your paths decide which techniques you can learn'],
      ['Where to find the books',
        '- Stronghold libraries\n'
        + '- Woodland mansions\n'
        + '- Pillager outposts\n'
        + '- Desert and jungle temples\n'
        + '- Dungeons'],
      ['Restrictions',
        '- Vampires and other undead can\'t learn Hamon from books\n'
        + '- Hamon and Anubis can\'t be used together'],
    ],
  },
  {
    id: 'vampire',
    title: 'Vampire',
    summary: 'Wear a bloodied Stone Mask at night',
    intro: 'Wear the **Stone Mask** to become a vampire. This is permanent.',
    sections: [
      ['Where to find the Stone Mask',
        '- Desert and jungle temples\n'
        + '- Ancient cities\n'
        + '- Stronghold corridors\n'
        + '- Woodland mansions'],
      ['How to use it',
        '1. Hold the mask in your off hand and kill a living mob\n'
        + '2. Wait until night\n'
        + '3. Put the mask on'],
      ['Downsides',
        '- Sunlight kills you\n'
        + '- Your blood drains over time, like hunger\n'
        + '- Blood only refills by feeding or killing'],
      ['Hamon',
        '- If you already know Hamon, you keep it\n'
        + '- Using a Hamon move as a vampire hurts you'],
    ],
  },
  {
    id: 'pillar-man',
    title: 'Pillar Man',
    summary: 'Wear the Progenitor Mask, then fall to a vampire',
    intro: 'You must be **human**. Vampires can\'t wear the mask.',
    sections: [
      ['How to become one',
        '1. Find a **Progenitor Mask** in a catacomb vault (rare)\n'
        + '2. Right click to press it to your face. You stay human for now\n'
        + '3. Get killed by a vampire, either a vampire mob or a vampire player\n'
        + '4. Instead of dying, you become a pool of blood that can\'t be hurt\n'
        + '5. After 30 seconds you rise again as a Pillar Man'],
      ['Born a Pillar Man',
        '- Every player has a 1% chance to start as a Pillar Man on their first join'],
      ['Modes',
        '- There are 3 Modes: Wind, Heat and Light\n'
        + '- Each is learned from a **Mode Book**\n'
        + '- Every shrine chest holds one Mode Book'],
      ['Downsides',
        '- Sunlight turns you to stone'],
    ],
  },
  {
    id: 'ultimate-lifeform',
    title: 'Ultimate Lifeform',
    summary: 'Wear the Stone Mask (Aja) as a Pillar Man',
    intro: 'You must be a **Pillar Man** first.',
    sections: [
      ['How to become one',
        '1. Craft a **Stone Mask (Aja)** from a Stone Mask and the Red Stone of Aja\n'
        + '2. Hold the mask in your off hand and kill a living mob\n'
        + '3. Wait until night\n'
        + '4. Put the mask on'],
      ['What changes',
        '- Sunlight no longer affects you\n'
        + '- You keep the Pillar Man moves and every Mode you learned\n'
        + '- You can gain abilities from the creatures you study'],
    ],
  },
  {
    id: 'anubis',
    title: 'Anubis',
    summary: 'A sword found in desert pyramids',
    intro: 'Anubis is a Stand inside a sword. You can have it alongside a normal Stand.',
    sections: [
      ['Where to find it',
        '- Desert pyramid chests only\n'
        + '- Stand Arrows never give Anubis\n'
        + '- Mobs never drop it'],
      ['How to use it',
        '- Right click the sword to bind it\n'
        + '- Once bound, any sword you hold counts as Anubis\n'
        + '- Sneak and right click twice to unbind it'],
      ['Restrictions',
        '- Anubis and Hamon can\'t be used together\n'
        + '- If another player kills you, Anubis drops for them'],
    ],
  },
  {
    id: 'fates',
    title: 'Fates',
    summary: 'What you are: Human, Vampire, Pillar Man or Ultimate Lifeform',
    intro: 'Your **Fate** is what your body is. Everyone starts **Human**. Your Fate is shown on the left of your menu.',
    sections: [
      ['The four Fates',
        '- **Human:** the default. Can learn Hamon\n'
        + '- **Vampire:** from the Stone Mask. Burns in sunlight, drinks blood\n'
        + '- **Pillar Man:** from the Progenitor Mask, or a 1% chance on your first join. Turns to stone in sunlight\n'
        + '- **Ultimate Lifeform:** a Pillar Man who wears the Stone Mask (Aja). Sunlight no longer hurts you'],
      ['How Fate changes',
        '- Human → Vampire\n'
        + '- Human → Pillar Man\n'
        + '- Pillar Man → Ultimate Lifeform\n'
        + '- None of these can be undone'],
      ['Fates and other powers',
        '- Any Fate can have a Stand\n'
        + '- A Fate is not a Discipline. A vampire can still know Hamon, but using a Hamon move hurts them\n'
        + '- Each Fate past Human has its own skill tree'],
      ['Related pages',
        '- Vampire, Pillar Man and Ultimate Lifeform have their own pages with the exact steps\n'
        + '- See Disciplines for Hamon and Anubis'],
    ],
  },
  {
    id: 'disciplines',
    title: 'Disciplines',
    summary: 'A trained power: Hamon or Anubis',
    intro: 'A **Discipline** is a power you train, on top of your Fate and your Stand. You can have one at a time.',
    sections: [
      ['The Disciplines',
        '- **None:** the default\n'
        + '- **Hamon:** learned from the skill tree or a Basic Hamon Book. Uses breath and comes in 8 paths\n'
        + '- **Anubis:** bound from the Anubis sword. Only found in desert pyramid chests'],
      ['Rules',
        '- Hamon and Anubis can\'t be held together\n'
        + '- A Discipline works with or without a Stand\n'
        + '- Your Discipline is shown on the left of your menu'],
      ['Changing it',
        '- Ask **Fate** in the Dream for **Forget a path**\n'
        + '- It removes your Discipline and its skill tree: all Hamon paths, or the bond with Anubis\n'
        + '- Afterwards you can learn the other one'],
      ['Related pages',
        '- Hamon and Anubis have their own pages\n'
        + '- See Fate (the Dream) for how wishes are paid'],
    ],
  },
  {
    id: 'fortunes',
    title: 'Fortunes',
    summary: 'A personality rolled on your first join',
    intro: 'Every player rolls a **Fortune** the first time they join. It is always on and changes how the world treats you. Hover yourself in the menu to read yours.',
    sections: [
      ['Wicked',
        '- Animals and villagers run from you\n'
        + '- Hostile mobs within 8 blocks start **Trembling** for 1 second, once every 30 seconds each\n'
        + '- Players near you see the edges of their screen dim\n'
        + '- Go a full day without a kill and your view starts to shake now and then, worse each day. A kill resets it'],
      ['Lucky',
        '- +1 Luck\n'
        + '- Better Stand Arrow odds\n'
        + '- Stat Crystals roll twice and keep the better result\n'
        + '- Now and then you feel pulled toward a structure\n'
        + '- Every Lucky player has a **phobia** that breaks their luck for 30 seconds:\n'
        + '  - **Tetraphobia:** a stack of exactly 4, 4 of one mob around you, or a 4th hit in a row\n'
        + '  - **Superstition:** the first hit from a Stand or the undead in a fight, or lightning nearby. Once a minute at most'],
      ['Bizarre',
        '- Wear 4 mismatched armour pieces or a full JoJo outfit to become **Charismatic**\n'
        + '- Charismatic: +1 Luck and 10% speed near other players, and 10% cheaper villager trades\n'
        + '- You get hints about meteors, structures and Stand users nearby\n'
        + '- Chests you loot sometimes hold an extra item'],
      ['Honorbound',
        '- You have **Honor** from 0 to 100, starting at 50\n'
        + '- Gain it by hitting from the front, waiting between attacks, fighting beside an ally, and fighting on below 30% health\n'
        + '- Lose it by shooting someone in the back, healing mid-fight, using ender pearls and stalling\n'
        + '- Above 75 Honor: +1 melee damage. Below 25: -1\n'
        + '- If you are close to beating someone, they get one second wind\n'
        + '- Food you give an opponent comes back to you\n'
        + '- If you die near an ally, they fight stronger for a while'],
      ['Cursed',
        '- No single hit can take your last half heart. A hit at half a heart still kills\n'
        + '- When you die you stay as a spirit for 60 seconds. You can fly, carry your items home or haunt your killer. You can\'t attack or use your Stand\n'
        + '- You start with one curse and can take more for their benefits:\n'
        + '  - **Blinded:** always blind, but anything making noise near you glows, even through walls\n'
        + '  - **Fragile:** double damage taken, but your spirit lasts twice as long, is invisible and keeps your Stand out\n'
        + '  - **Legless:** you crawl and can\'t jump, but every hit lands 2 seconds late'],
      ['Idealogue',
        '- In a one-on-one fight:\n'
        + '  - 15% more damage to foes with more max health than you\n'
        + '  - 10% more damage to Stand users\n'
        + '  - 15% less damage taken below 40% health\n'
        + '- Winning a duel gives XP, heals 60% and gives Regeneration II\n'
        + '- 2 days without a fight lowers your Luck and attack speed until you fight again\n'
        + '- Mobs born with a Stand come after you\n'
        + '- Every 10 levels makes you faster and mine faster out of combat, up to 3 times'],
      ['Changing your Fortune',
        '- Ask **Fate** in the Dream for **Another fortune**. You never get the same one back\n'
        + '- The **Skull of Fortune** charm makes your Fortune stronger, both the good and the bad parts'],
    ],
  },
  {
    id: 'traits',
    title: 'Traits',
    summary: 'A small bonus rolled with your character',
    intro: 'Every player also rolls one **Trait**. Traits are small and always on. Two of them are **legendary** and rare, with about a 3% chance.',
    sections: [
      ['Common traits',
        '- **Silent Footsteps:** your footsteps make no sound\n'
        + '- **Two Right Hands:** melee hits have a 20% chance to strike again for half damage. 3 of those within 5 seconds make your next hit a **Double Blow** for 125% damage and knockback\n'
        + '- **Running Style:** you speed up the longer you sprint, up to 18% faster after 5 seconds\n'
        + '- **Secret Technique:** below 30% health you move 20% faster\n'
        + '- **Killer\'s Luck:** changes each day. Good days hit harder with more luck, bad days give nothing'],
      ['Legendary traits',
        '- **Joestar Birthmark:** while you have a Stand, a worthy player who stays near you can wake a Stand of their own\n'
        + '- **Devil\'s Blessing:** you deal more damage to players with high Worthiness'],
      ['Changing your trait',
        '- Ask **Fate** in the Dream for **Another trait**\n'
        + '- You never get the same trait back\n'
        + '- Paying more raises the odds of a legendary one, up to 7 times the normal chance'],
    ],
  },
  {
    id: 'fate',
    title: 'Fate (the Dream)',
    summary: 'A figure you meet in your sleep who grants wishes',
    intro: '**Fate** is a figure of gold light that you meet in the **Dream**. It looks like you. It trades wishes for items.',
    sections: [
      ['Getting there',
        '- Sleep in a bed. About 1 night in 12 you are taken into the Dream\n'
        + '- It can\'t happen more than once every 3 days\n'
        + '- Eating a **Lucid Lily** before sleeping always takes you there'],
      ['How it works',
        '- Fate talks to you and offers wishes\n'
        + '- You answer by touching the pillars of light that rise from the water\n'
        + '- It remembers you: what you asked for and what you paid\n'
        + '- Touch **Nothing. Let me wake.** to leave'],
      ['Paying',
        '- Drop items into the water to pay. The candles around you light up as you give\n'
        + '- White candles mean you have paid the least price. Yellow ones mean you are paying extra\n'
        + '- Anything of value counts. A diamond is the basic unit. Iron is worth a quarter, a Stone Mask over 4 and a Stand Arrow 12.5\n'
        + '- **Take my memories instead** pays with XP\n'
        + '- Items it doesn\'t use are handed back'],
      ['Wishes',
        '- **Something bright:** treasure and mod items, never worth less than you paid. 1.5 to 15 diamonds\n'
        + '- **Another trait:** rerolls your trait. 4.5 to 17.5 diamonds\n'
        + '- **More of yourself:** raises one of your 5 stats, you pick which. 1 point per diamond, up to 4\n'
        + '- **More of your Stand:** the same, for one of your Stand\'s stats\n'
        + '- **A hollow where it was:** removes your Stand, its tree and its bar. 12.5 diamonds\n'
        + '- **A Stand of your choosing:** only without a Stand. Your next arrow gives the Stand you name half the time, up to 9 in 10 if you pay more. 15 to 30 diamonds'],
      ['More wishes',
        '- **Another fortune:** rerolls your Fortune. 5 diamonds\n'
        + '- **Forget a path:** removes your Discipline and its tree. 5 diamonds\n'
        + '- **Another night:** gives you a Lucid Lily. 3 diamonds'],
    ],
  },
  {
    id: 'combat',
    title: 'Combat',
    summary: 'Attacking, guarding, parrying, dodging and Stand energy',
    intro: 'Combat is built around timing and movement. Every hit is capped at **10 damage**, so a fight is won by reading your opponent, not by one big number.',
    sections: [
      ['Attacking',
        '- With your Stand out, left click makes your **Stand** punch, even at the air\n'
        + '- Holding a weapon or tool keeps your own swing instead\n'
        + '- Your moves go on the **combat bar**. Press the Stand skill keys to use them, and the modifier key for the second row\n'
        + '- The Player tree has **Heavy Punch**, **Barrage**, **Dodge**, **Double Jump** and **Burst**. They work with or without a Stand'],
      ['Guarding with your Stand',
        '- Stand out, empty main hand, hold **right click**\n'
        + '- Your Stand steps in front of you and takes the blows\n'
        + '- Hits from the front are blocked, hits from the side do half damage, hits from behind go through\n'
        + '- Each blocked hit costs Stand energy\n'
        + '- The guard breaks after 8 blows, more with perks. Heavy hits count for more\n'
        + '- A broken guard leaves you **Guard Broken** for 3 seconds'],
      ['Parrying',
        '- Without your Stand guarding, tap **right click** with an empty hand or a sword\n'
        + '- The window is **0.1 seconds**, right before the hit lands\n'
        + '- A parry staggers the attacker and pushes them back\n'
        + '- Your next hit within 1.5 seconds deals 25% more\n'
        + '- A parry in the very first tick is **perfect** and also deals 2 damage back\n'
        + '- Miss, and you can\'t parry again for 2 seconds. Land it, and you can parry again after 1 second, a little longer for each parry in a row\n'
        + '- Barrages can\'t be parried. Arrows and other shots can only be parried with a sword\n'
        + '- Nothing can be parried from behind'],
      ['Moving',
        '- **Dodge** uses dash charges and **Burst** uses leap charges\n'
        + '- You have 2 of each. They come back over 7 seconds\n'
        + '- With your Stand out, Dodge becomes **Stand Dash** and Burst becomes **Stand Leap**'],
      ['Stand energy',
        '- Summoning costs 10% of your Stand bar, and you need at least 25% to summon\n'
        + '- In a fight a full bar drains in about 2 minutes just from being out, before move costs. Out of a fight it lasts about 5 minutes\n'
        + '- Put your Stand away and the bar refills in about 35 seconds, or about 2 minutes during a fight\n'
        + '- Your Stand\'s punches give a little energy back\n'
        + '- At an empty bar your Stand vanishes'],
      ['Stances',
        '- Hold the summon key to change stance\n'
        + '- **Full:** your Stand fights\n'
        + '- **Ready stance:** your Stand stays half out. It doesn\'t punch, but it catches one hit every 5 seconds and knocks away shots every 2 seconds. Guarding in this stance refills energy\n'
        + '- **Utility:** your Stand helps with blocks and chores instead of fighting'],
      ['Barrages and clashes',
        '- A barrage holds its target in place\n'
        + '- Its damage is saved up and lands all at once when the barrage ends, so nothing dies halfway through\n'
        + '- When two Stand barrages meet they **clash**. Mash attack to break through first'],
      ['Stands and damage',
        '- Only a Stand attack can hit a Stand. Every hit on a Stand goes straight to its user\n'
        + '- A user and their Stand count as one target, so you can\'t be hit twice at once by both\n'
        + '- Players without a Stand can\'t see Stands at all'],
      ['Armour',
        '- Against other players, armour blocks between 20% and 55% of damage, depending on your armour, toughness and Protection\n'
        + '- This keeps well-geared players tough without making them unbeatable'],
    ],
  },
  {
    id: 'status-effects',
    title: 'Status Effects',
    summary: 'Stun, Bleeding, Burning and the rest',
    intro: 'Many moves leave a **status** on what they hit. Hover a move in your menu and its **Inflicts** line tells you which. Mobs can use them on you too.',
    sections: [
      ['Stunned',
        '- **Stun I:** slowed, hitting half as hard, and only moves that answer a hit still work. A parry or a guard can still save you\n'
        + '- **Stun II:** held in place with no moves at all\n'
        + '- Players are never stunned for more than 1.5 seconds\n'
        + '- Uppercut, Star Finger, Ground Breaker and Riposte are some of the moves that stun'],
      ['Bleeding',
        '- Lasts 5 seconds\n'
        + '- Each new wound deepens it, up to 4 levels\n'
        + '- When it runs out it deals its damage all at once: 3, 6, 9 or 10\n'
        + '- Vampire bites, Knife Barrage, Blade Launch and Sabre Cycle are some of the moves that cause it\n'
        + '- A heavy bleed running out can cost you a limb'],
      ['Burning',
        '- Stacks up to 3 times\n'
        + '- Deals 1, 1.5 or 2 damage a second for 5 seconds. Each new stack starts the timer again\n'
        + '- Water and Fire Resistance put it out\n'
        + '- Comes from Magician\'s Red and Scarlet Overdrive'],
      ['Guard Broken',
        '- You can\'t guard, parry or raise a shield\n'
        + '- Lasts 3 seconds\n'
        + '- Caused by breaking a Stand guard or by moves that break shields'],
      ['Trembling',
        '- Slows you by 35%, but you can still attack\n'
        + '- Hostile mobs get it near a **Wicked** player'],
      ['Tangled',
        '- Blocks 3 of your moves for 2.5 seconds\n'
        + '- Comes from Hermit Purple\'s thorns'],
      ['Resistance',
        '- A mob stunned again within 3 seconds of its last stun only gets half of it\n'
        + '- Bosses like the Wither, the Warden and the Ender Dragon only take half of every status'],
      ['Helpful effects',
        '- **Keen Guard** from Speedwagon Tonic: your parry window lasts 1 tick longer\n'
        + '- **Stand Vigor** from Coffee Flavored Gum: Stand energy refills faster\n'
        + '- **Steady Breath** from Zeppeli Wine: breath recovers faster until you are hit'],
    ],
  },
  {
    id: 'accessories',
    title: 'Accessories',
    summary: 'Gloves, rings, charms and relics you wear',
    intro: 'Accessories are worn in the panel beside your inventory. Open it with the **glove button**. Each one fits one slot and gives one effect.',
    sections: [
      ['Slots',
        '- **Head:** hats, masks and circlets\n'
        + '- **Neck:** amulets, pendants and scarves\n'
        + '- **Gloves:** gloves and gauntlets\n'
        + '- **Ring:** rings and bands\n'
        + '- **Back:** capes, cloaks and coat chains\n'
        + '- **Charm:** charms and trinkets'],
      ['Rarity and Burdens',
        '- From weakest to strongest: **Common**, **Uncommon**, **Rare**, **Legendary**, **Ancient**\n'
        + '- Some accessories have a **Burden**, a downside that comes with the bonus\n'
        + '- Some only work for one Stand, or only for Pillar Men. The tooltip says which\n'
        + '- All of them are random chest loot. Rarer ones are harder to find, and labyrinth chests have the most'],
      ['Common',
        '- **Joseph\'s Fingerless Gloves** (Gloves): after a perfect parry, your next hit within a second deals 2 more\n'
        + '- **Jotaro\'s Coat Chain** (Back): 25% less knockback\n'
        + '- **Speedwagon\'s Bowler Hat** (Head): sprint 15% faster below 30% health\n'
        + '- **Bubble Gloves** (Gloves): needed for Bubble Launcher and Bubble Cutter\n'
        + '- **Lisa Lisa\'s Scarf** (Neck): needed for Snake Muffler'],
      ['Uncommon',
        '- **Caesar\'s Headband** (Head): Bubble Cutter fires 2 bubbles a shot, and all your bubbles fly 30% faster\n'
        + '- **Kakyoin\'s Cherry Earrings** (Head, Hierophant Green): Emerald Splash fires 2 more emeralds\n'
        + '- **Polnareff\'s Earrings** (Head, Silver Chariot): Afterimages makes 2 more images'],
      ['Rare',
        '- **Jotaro\'s Cap** (Head, Star Platinum): once your guard takes 3 hits in 5 seconds, your next Stand punch staggers\n'
        + '- **Sherry\'s Locket** (Neck, Silver Chariot): a landed Riposte readies Balestra at once\n'
        + '- **Hamon Control Mask** (Head): Hamon breath refills 25% faster. Burden: you can\'t eat or drink while wearing it'],
      ['Rare charms',
        '- **Star Mirror:** every parry window lasts 1 tick (0.05 s) longer\n'
        + '- **Creative Cube:** Utility stance costs no energy\n'
        + '- **Skull of Fortune:** your Fortune is stronger, both the good and the bad parts\n'
        + '- **Warding Charm:** mobs with Stands stop gathering around you\n'
        + '- **Fractured Shoulder Plate:** keeping your Stand out drains 5% less'],
      ['Legendary',
        '- **Ring of Aja** (Ring): Hamon deals 20% more in sunlight. Burden: 20% less at night or under cover\n'
        + '- **DIO\'s Headband** (Head, The World): each mob you kill in stopped time takes 3 seconds off Time Stop\'s cooldown, up to half of it\n'
        + '- **Wamuu\'s Headdress** (Head, Pillar Men): while sprinting, a wind shell turns back arrows and shots from behind'],
      ['Ancient',
        '- **Wedding Ring of Death** (Ring): every hit you land deals 15% more. Burden: go 3 minutes without hitting a hostile mob and it gives you Poison II for 5 seconds'],
    ],
  },
  {
    id: 'consumables',
    title: 'Consumables',
    summary: 'Drinks, snacks and trinkets found in chests',
    intro: 'Small items found in chests all over the world. About 1 chest in 5 has one. They stack to 16.',
    sections: [
      ['Drinks and food',
        '- **Speedwagon Tonic:** your parry window lasts 1 tick longer for 3 minutes\n'
        + '- **Coffee-Flavored Gum:** Stand energy refills 50% faster for 1 minute\n'
        + '- **Zeppeli\'s Wine:** breath refills 50% faster for 2 minutes. Getting hit spills it\n'
        + '- **Cherries:** your dodge and dash charges come back at once\n'
        + '- **Blood Vial:** refills some blood for a vampire. Makes anyone else sick\n'
        + '- **Lucid Lily:** your next sleep takes you to the Dream. See Fate (the Dream)'],
      ['Trinkets',
        '- **Tarot Card:** draws one of 11 cards, each a potion effect. Most are good. The Tower and Death are not\n'
        + '- **Lucky Coin:** heads gives Luck for 5 minutes, tails gives Bad Luck for 1 minute\n'
        + '- **Torn Manga Page:** gives a little experience toward a random stat\n'
        + '- **Road Roller Key:** does nothing. Willow buys it anyway'],
      ['Stat Crystal',
        '- Raises a random stat by 1 to 3 levels. It can pick one of your stats or one of your Stand\'s\n'
        + '- Rare in chests. Vampires sometimes drop one\n'
        + '- Lucky players roll it twice and keep the better result\n'
        + '- It isn\'t used up if every stat is already maxed'],
    ],
  },
  {
    id: 'weapons',
    title: 'Weapons and Enchantments',
    summary: 'Daggers, rapiers, sabres, the scythe and more',
    intro: 'Every weapon type plays differently. The tooltip on each weapon explains its type.',
    sections: [
      ['Swords',
        '- Work like vanilla swords and hit everything standing next to your target\n'
        + '- Any blade can parry. Right click with it in hand'],
      ['Daggers',
        '- Deal double damage to anything that isn\'t already fighting you\n'
        + '- Work thrown or swung\n'
        + '- Don\'t hit nearby mobs\n'
        + '- Wood, stone, iron, gold, diamond, netherite and meteorite'],
      ['Rapiers',
        '- 0.75 blocks more reach and ignore 10% of armour\n'
        + '- Tap to lunge 4 blocks, ignoring 30% of armour\n'
        + '- One target at a time\n'
        + '- Wood to meteorite'],
      ['Sabres',
        '- Built for parrying. After a perfect parry, your next parry is perfect too\n'
        + '- Don\'t hit nearby mobs\n'
        + '- Wood to meteorite'],
      ['Meteorite Scythe',
        '- Right click to hit everything within 3 blocks for 6 damage, with knockback\n'
        + '- 2.5 second cooldown\n'
        + '- Can\'t parry, and normal swings don\'t hit nearby mobs'],
      ['Hamon weapons',
        '- **Clackers:** boost Hamon damage while held. Swing to throw them, hold right click to spin them. Come in 9 colours\n'
        + '- **Pluck:** a sword sold by Willow and found in ruins'],
      ['Ancient Bow',
        '- The only bow that can fire a Stand Arrow\n'
        + '- A player it hits rolls for a Stand as if stabbed. A mob it hits may wake a Stand of its own\n'
        + '- Found in ancient ruins in the Phantom Moors'],
      ['Enchantments',
        '- **Conductivity** (weapons, up to III): Hamon users deal 3 more damage per level to vampires and the undead\n'
        + '- **Siphon** (weapons, up to V): each kill heals you a little. Vampires also get some blood back\n'
        + '- **Luck and Pluck** (armour, up to III): when you drop below 30% health you get **Pluck** for 8 seconds: 1.5 more damage and 10% knockback resistance per level'],
    ],
  },
  {
    id: 'items',
    title: 'Items and Materials',
    summary: 'Arrows, masks, meteorite gear, outfits and other key items',
    intro: 'The mod\'s main items, what they do and where they come from.',
    sections: [
      ['Arrows',
        '- **Stand Arrow:** stab yourself to wake a Stand. Needs Worthiness 15\n'
        + '- **Stand Arrow Shard:** the same with worse odds. Burns out if it fails\n'
        + '- **Lucky Arrow:** with a Stand, gives it a new skin\n'
        + '- **Requiem Arrow:** does nothing yet. Requiem comes in the Part 5 update\n'
        + '- **Heaven\'s Arrow:** does nothing yet. Very valuable for trading'],
      ['Masks',
        '- **Stone Mask:** makes you a vampire. See Vampire\n'
        + '- **Red Stone of Aja:** combine with a Stone Mask to make the **Stone Mask (Aja)**\n'
        + '- **Progenitor Mask:** the first step to becoming a Pillar Man. See Pillar Man'],
      ['Meteorites',
        '- A **Meteor Chart** shows where a meteor will land. Be there and mine what falls\n'
        + '- Raw meteorite is crafted into **Meteorite Ingots** with an **Ancient Arrowhead**\n'
        + '- Ingots upgrade netherite armour, tools and weapons at a smithing table, with a **Meteorite Tablet** as the template\n'
        + '- Tablets are found in ancient ruins. Copy one with an Ancient Arrowhead and a Meteorite Ingot'],
      ['Outfits',
        '- **Soul Chips** turn armour into JoJo outfits at a smithing table. The chip is the template\n'
        + '- 11 outfits: Jotaro, Kakyoin, Polnareff, Avdol, Joseph, DIO, Zeppeli, Jonathan, Dio Brando, Young Joseph and Caesar\n'
        + '- Sold by Willow and found in ancient ruins and labyrinth vaults\n'
        + '- A full outfit counts for the Bizarre Fortune'],
      ['Skins and discs',
        '- A skin you only wear is lost when you switch away from it\n'
        + '- Use a **Memory Disc** while your Stand wears a skin, then play the disc to keep that skin for good\n'
        + '- Kept skins can be worn again from the Skins page of your menu\n'
        + '- A **Stand Disc** can hold a whole Stand. Play it to take that Stand back'],
      ['Respawn Totem',
        '- Found out in the world. Wake it with a **Totem Key** and it will bring you back 3 times\n'
        + '- Its eyes show your lives left: red 3, orange 2, yellow 1\n'
        + '- Your bed spawn isn\'t changed\n'
        + '- Waking a totem uses up the key. Keys are in ancient ruins, and Willow sells them'],
      ['Other items',
        '- **Family Record:** use it on a player to invite them as your descendant. Relatives sense each other nearby, and when one wakes a Stand, a descendant without one may wake one too\n'
        + '- **Jar:** holds a vampire\'s severed head and keeps it safe from the sun\n'
        + '- **Path Book:** teaches a Hamon path. The Hermit one is the **Basic Hamon Book**\n'
        + '- **Mode Book:** teaches a Pillar Man Mode\n'
        + '- **JoJo Coin:** Willow\'s currency\n'
        + '- **Music Discs:** 11 songs from the series'],
    ],
  },
  {
    id: 'willow',
    title: 'Willow',
    summary: 'A trader in the Phantom Moors who pays in JoJo Coins',
    intro: '**Willow** is a trader found in the **Phantom Moors**, a grey biome found inland where it is cool. Her currency is the **JoJo Coin**.',
    sections: [
      ['What she buys',
        '- Stand Arrows and shards\n'
        + '- Stone Masks\n'
        + '- Ancient Arrowheads and raw meteorite\n'
        + '- Emeralds\n'
        + '- Road Roller Keys'],
      ['What she sells',
        '- **From the start:** raw meteorite, Ancient Arrowheads and Soul Chips\n'
        + '- **After some trades:** shards, Meteorite Ingots and Totem Keys\n'
        + '- **After more:** Stone Masks, Lucky Arrows, Bubble Gloves and Meteor Charts\n'
        + '- **Once she fully trusts you:** Pluck and Lisa Lisa\'s Scarf'],
      ['Forging an arrow',
        '- Once she fully trusts you, she forges a Stand Arrow from 6 shards and 3 Meteorite Ingots'],
      ['Tips',
        '- Every trade raises her trust\n'
        + '- New stock is added at each step, and nothing is ever taken away\n'
        + '- No two Willows have the same stock. If one doesn\'t sell what you need, find another\n'
        + '- Ancient ruins nearby have chests with coins, shards, arrowheads and Totem Keys'],
    ],
  },
];

export const FIRST_PAGES = ['stands', 'hamon', 'vampire', 'pillar-man', 'ultimate-lifeform', 'anubis'];

export const MAX_PAGES = 25;
export const MAX_SECTIONS = 10;

const PAGE_ID = /^[a-z0-9-]{1,32}$/;

export function findPage(id, pages = PAGES) {
  const wanted = String(id ?? '').trim().toLowerCase();
  return pages.find((page) => page.id === wanted)
    ?? pages.find((page) => page.title.toLowerCase() === wanted)
    ?? null;
}

export function searchPages(pages, text) {
  const wanted = String(text ?? '').trim().toLowerCase();
  return pages.filter((page) => page.id.includes(wanted) || page.title.toLowerCase().includes(wanted));
}

export function formatSections(sections) {
  return sections.map(([name, value]) => `## ${name}\n${value}`).join('\n\n');
}

export function parseSections(text) {
  const sections = [];
  let current = null;
  for (const line of String(text ?? '').replace(/\r/g, '').split('\n')) {
    const heading = line.match(/^##\s*(.*)$/);
    if (heading) {
      current = [heading[1].trim(), []];
      sections.push(current);
    } else if (current) {
      current[1].push(line);
    } else if (line.trim()) {
      return null;
    }
  }
  return sections.map(([name, lines]) => [name, lines.join('\n').trim()]);
}

export function checkPage(raw, pages) {
  const id = String(raw.id ?? '').trim().toLowerCase();
  const title = String(raw.title ?? '').trim();
  const summary = String(raw.summary ?? '').trim();
  const intro = String(raw.intro ?? '').trim();
  const sections = parseSections(raw.sections);
  if (!PAGE_ID.test(id)) {
    return { error: 'The id must be 1-32 lowercase letters, digits or dashes, such as `pillar-man`.' };
  }
  if (!pages.some((page) => page.id === id) && pages.length >= MAX_PAGES) {
    return { error: `There can be at most ${MAX_PAGES} pages. Remove one first.` };
  }
  if (!title || title.length > 100) {
    return { error: 'The title needs 1-100 characters.' };
  }
  if (!summary || summary.length > 100) {
    return { error: 'The summary needs 1-100 characters.' };
  }
  if (!intro || intro.length > 1000) {
    return { error: 'The intro needs 1-1000 characters.' };
  }
  if (sections === null) {
    return { error: 'Start each section with a heading line such as `## How to become one`.' };
  }
  if (sections.length > MAX_SECTIONS) {
    return { error: `A page can have at most ${MAX_SECTIONS} sections.` };
  }
  for (const [name, value] of sections) {
    if (!name || name.length > 256) {
      return { error: 'Each section heading needs 1-256 characters.' };
    }
    if (!value || value.length > 1024) {
      return { error: `The section "${name}" needs 1-1024 characters under its heading.` };
    }
  }
  return { page: { id, title, summary, intro, sections } };
}
