# HopperFilter
Eliminate complicated hopper setups with this little plugin. Filter items through hoppers by simply renaming the hopper in an anvil and placing it.

Filter patterns are in the following format: `*wood,^cobble,$log,*deep&*slate,#villager_plantable_seeds`

- Split your pattern's rules with `,`
- Prefix your pattern to enforce specific conditions
  - An asterisk (*) means it must contain that string
  - A caret (^) means the item name must start with the specified pattern
  - A dollar sign ($) means the item name must end with the specified pattern
  - An octothorpe (#) specifies the item must have a specific tag such as #minecraft:villager_plantable_seeds, the "minecraft:" part is not necessary
- A pattern with no prefix means that the item name must exactly match the specified pattern
- You can perform AND logic through the use of an ampersand (&) between your patterns

Keep in mind that since this prevents items from entering a hopper, if an item is in the 0th index of a hopper with no other hopper to move it out, it will jam your sorters.

## Credits
Thanks to LiveOverflow for the inspiration for this plugin in [this](https://youtu.be/Gi2PPBCEHuM?t=224) video.