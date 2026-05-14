# Mob Stacker Advanced

NeoForge/Fabric 1.21.1 server/client mod that stacks configured mobs into one entity.

After the first run, edit:

```text
config/mobstackeradv.json
```

Useful options:

```json
{
  "entityIds": ["minecraft:cow", "minecraft:pig", "minecraft:sheep", "minecraft:chicken"],
  "stackRadius": 8.0,
  "minimumGroupSize": 2,
  "maxStackSize": 64,
  "scanIntervalTicks": 40,
  "showStackName": true,
  "stackParticleId": "minecraft:poof",
  "separatorItemId": "minecraft:stick",
  "consumeSeparatorItem": false,
  "separatedNoStackTicks": 200
}
```

Build jars:

```text
./gradlew :neoforge:build
./gradlew :fabric:build
./gradlew :forge:build
```

On Java 17, the `forge-1.20.1` branch skips the Java-21-only NeoForge/Fabric modules during Gradle configuration so Forge 1.20.1 can still build normally.

Rules:

- Only entity ids in `entity_ids` stack.
- Only mobs within `stack_radius` merge.
- A new stack starts only when `minimum_group_size` matching mobs are close enough.
- Baby mobs stack separately from adults.
- Sheep stack separately by wool color and sheared state.
- Shearing a woolly sheep stack drops wool once per sheep represented in that stack and damages shears for each sheep sheared.
- Milking a cow stack with multiple buckets fills one milk bucket per milkable cow, limited by the empty buckets in hand.
- When a chicken stack lays an egg, it drops one egg per adult chicken represented in that stack.
- Right-clicking a stack with `separator_item_id` splits one mob out of the stack.
- A manually separated mob waits `separated_no_stack_ticks` before it can stack again.
- Right-clicking a stack with a named name tag names one mob split from that stack, and named mobs do not stack.
- Feeding a stacked adult animal consumes one breeding food per breedable mob in that stack, limited by the item count in hand.
- Stacked animals remember how many represented mobs are in love, then breed in pairs inside the stack or with a nearby compatible love stack.
- Mobs in a stack that already bred have their own cooldown, so partially feeding a stack does not block the whole stack.
- When a stacked mob dies, vanilla loot and XP drop for the killed mob, then the remaining count stays as a new stack.
- Fire, lava, campfire, and magma-style deaths only remove one mob from the stack; the remaining stack gets brief protection so it does not instantly chain-die.
