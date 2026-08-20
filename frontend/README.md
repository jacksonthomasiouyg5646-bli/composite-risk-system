# 前端启动

PowerShell 会优先执行 `npm.ps1`，当前系统禁止运行 `.ps1`，所以不要在 PowerShell 里直接执行 `npm run dev`。

使用下面命令启动：

```powershell
.\dev.cmd
```

或：

```powershell
npm.cmd run dev
```

如果提示 `'vite' 不是内部或外部命令`，说明依赖还没安装，先执行：

```powershell
npm.cmd install --cache "$env:TEMP\user-management-npm-cache"
```

`dev.cmd` 会自动检查并安装依赖。

在 VS Code 中可以直接运行任务：

```text
Terminal -> Run Task -> Frontend: dev
```
