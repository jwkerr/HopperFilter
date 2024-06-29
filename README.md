# HopperFilter
Eliminate complicated hopper setups with this little plugin. Filter items through hoppers by simply renaming the hopper in an anvil and placing it.

Filter patterns are in the following format: `*wood*,*cobble,log*,deepslate`

- Split your pattern's rules with `,`
- Asterisks surrounding a pattern mean it must contain that string
- An asterisk at the start or end of a pattern means the item name must either start with or end with the specified pattern
- A pattern with no asterisks means that the item name must exactly match the specified pattern

Keep in mind that this both prevents items from entering AND leaving a hopper. If an item is in the 0th index of a hopper with no other hopper to move it out, it will jam your sorters.

## Credits
Thanks to LiveOverflow for the inspiration for this plugin in [this](https://youtu.be/Gi2PPBCEHuM?t=224) video.