$llm_dir = (Split-Path -Parent (python -c "import bigdl.llm;print(bigdl.llm.__file__)"))
$lib_dir = Join-Path $llm_dir "libs"

$model_family = ""
$threads = 8
$n_predict = 128

# Function to display help message
function Display-Help
{
    Write-Host "usage: ./llm-cli.ps1 -x MODEL_FAMILY [-h] [args]"
    Write-Host ""
    Write-Host "options:"
    Write-Host "  -h, --help           show this help message"
    Write-Host "  -x, --model_family {llama,bloom,gptneox}"
    Write-Host "                       family name of model"
    Write-Host "  -t N, --threads N    number of threads to use during computation (default: 8)"
    Write-Host "  -n N, --n_predict N  number of tokens to predict (default: 128, -1 = infinity)"
    Write-Host "  args                 parameters passed to the specified model function"
}

function llama
{
    $command = "$lib_dir/main-llama.exe -t $threads -n $n_predict $filteredArguments"
    Write-Host "$command"
    Invoke-Expression $command
}

function bloom
{
    $command = "$lib_dir/main-bloom.exe -t $threads -n $n_predict $filteredArguments"
    Write-Host "$command"
    Invoke-Expression $command
}

function gptneox
{
    $command = "$lib_dir/main-gptneox.exe -t $threads -n $n_predict $filteredArguments"
    Write-Host "$command"
    Invoke-Expression $command
}

function starcoder
{
    $command = "$lib_dir/main-starcoder.exe -t $threads -n $n_predict $filteredArguments"
    Write-Host "$command"
    Invoke-Expression $command
}

# Remove model_family/x parameter
$filteredArguments = @()
for ($i = 0; $i -lt $args.Length; $i++) {
    if ($args[$i] -eq '--model_family' -or $args[$i] -eq '--model-family' -or $args[$i] -eq '-x')
    {
        if ($i + 1 -lt $args.Length -and $args[$i + 1] -notlike '-*')
        {
            $i++
            $model_family = $args[$i]
        }
    }
    elseif ($args[$i] -eq '--threads' -or $args[$i] -eq '-t')
    {
        $i++
        $threads = $args[$i]
    }
    elseif ($args[$i] -eq '--n_predict' -or $args[$i] -eq '--n-predict' -or $args[$i] -eq '-n')
    {
        $i++
        $n_predict = $args[$i]
    }
    else
    {
        $filteredArguments += "`'" + $args[$i] + "`'"
    }
}

# Perform actions based on the model_family
switch ($model_family)
{
    "llama" {
        llama
    }
    "bloom" {
        bloom
    }
    "gptneox" {
        gptneox
    }
    "starcoder" {
        starcoder
    }
    default {
        Write-Host "Invalid model_family: $model_family"
        Display-Help
    }
}
