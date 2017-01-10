$ScriptBlock = {
    while($true) {
        $HTTP_Request = [System.Net.WebRequest]::Create('http://localhost:8080/catgenome')

        Try {
            $HTTP_Response = $HTTP_Request.GetResponse()
        } 
        Catch {
            Write-Host "Waiting for NGGB to start"
            Start-Sleep -s 10
            Continue
        }
        $HTTP_Status = [int]$HTTP_Response.StatusCode

        If ($HTTP_Status -eq 200) { 
            Write-Host "NGGB is running at http://localhost:8080/catgenome"
            start 'http://localhost:8080/catgenome'
        }
        Else {
            Write-Host "Waiting for NGGB to start"
            $HTTP_Response.Close()
            Continue
        }

        $HTTP_Response.Close()
        break
    }
}

$jobId = Start-Job $ScriptBlock

try 
{
    Test-Path './start.bat'
    ./start.bat
}
finally
{
    ./stop.bat
}

