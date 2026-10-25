
# PDM Library Management Project

Please use **Java JDK version: 25.0.3 or above**.
## 1. Build and run (for marker)
1. Edit `db.properties` at the project root to configure database settings:
    + `db.url`: JDBC connection URL
    + `db.user`: Database username
    + `db.password`: Database user password
2. If using VSCode, press `Ctrl` + `Shift` + `P` then find and choose `Java: Configure Java Runtime` -> choose **JavaSE 25** or above then `Apply Settings`
3. ...

## 2. Git and GitHub contribution tutorial (for members)

- Install Git: <https://git-scm.com/>
- In VS Code, sign in to GitHub.
- Open the **Source Control** tab (branch/tree icon).
- Click **Clone Repository**, choose `Clone from Github` then `libm`, then choose a folder to save the project.
- Remember that folder, it is your project working place.
- Next time you code, open the folder `libm` in this folder in VS Code.
- Git quick cmd guide: <https://github.com/git-guides>, or use GUI tutorial below

### Git Configuration - for contribution to be record

Before You use git, enter command in terminal(replace with your github username and github email-> go github settings and find your unique GitHub no-reply address it looks like ID+username@users.noreply.github.com):

```bash
git config --global user.name "Your Name"
```
```bash
git config --global user.email "your.email@example.com"
```


### Commit and Push in VS Code (GUI)
**Remember** to always **pull** before code.

1. After editing files, open **Source Control**.
2. Click "+" next to each file you want to save the change to the server.
3. Enter a commit message( what you just code or do).
4. Click the dropdown button "v" near **Commit**, then choose **Commit & Push**.
5. To get latest changes from teammates, click the three dot "..." near the repository title "libm" in Source control tab then pull.


## 3. Scene builder (for members)

- Download: <https://gluonhq.com/products/scene-builder/>
- Configuration: On taskbar, click `Preview` -> `Theme` -> `Modena(FX8)`
- For choosing UI component: avoid using **Gluon** components - the component in **blue color**.

## 4. Configuration (for members)
- Remember to change the Project Java Runtime - see step 2 of **Build and run (for marker)**
- Goto `.vscode\setting.json`and change **java.configuration.updateBuildConfiguration** to **automatic**

```
"java.configuration.updateBuildConfiguration": "automatic" 
```