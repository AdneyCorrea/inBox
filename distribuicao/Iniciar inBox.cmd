@echo off
setlocal
cd /d "%~dp0"
title inBox - mantenha esta janela aberta
if not exist "runtime\bin\java.exe" (
  echo Extraia todo o ZIP antes de iniciar o inBox.
  pause
  exit /b 1
)
echo Iniciando inBox. Aguarde o navegador abrir.
echo Para encerrar, pressione Ctrl+C ou feche esta janela.
"runtime\bin\java.exe" -jar "app\inbox.jar" --server.address=127.0.0.1 --server.port=0 --inbox.abrir-navegador=true --inbox.dados-diretorio=catalogo-local
if errorlevel 1 (
  echo Nao foi possivel iniciar o inBox. Confira o erro acima.
  pause
)
