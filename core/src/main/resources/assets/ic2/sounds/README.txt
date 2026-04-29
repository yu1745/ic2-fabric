IC2 Sound Resource Folder

In order to make the project more tidy, here we have standardized the naming and path of sound files.
If you want to append sound for IC2, please make sure you have worked on the following requirements:

# The name of all folders and sound ogg files need to use lower case
# Minimize folder hierarchy as much as possible, generally controlled under 3 - 4
# Describe this folder/sound file in one word if possible. If there are too many folder levels, as a last resort, snake-like naming can be used appropriately

# The following are the naming conventions for some specific content

## Tools

- Folder: sounds/tools
- Path
    - Generally, it's usually like: "ic2:tools/[Tool Name]", for example: "ic2:tools/wrench".
    - If there is an electric upgrade version of this tool, please create a subfolder named after the tool under the tools folder. 
      And put in the normal version of the tool sound (named as this tool's name) and the electric version of the sound (named "electric")
- Name 
    - "item.[Tool Name].[Action]" For example:
        - "item.treetap.use"
        - "item.wrench.use"
        - "item.chainsaw.idle"
    - "item.[Tool Name].electric.[Action]" For example: 
        - "item.treetap.electric.use"

[WIP]