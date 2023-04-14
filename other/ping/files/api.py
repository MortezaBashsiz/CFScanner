from flask import Flask, jsonify, abort
from datetime import date

today = date.today()

token="a50ced64-6eba-4ab8-a30d-3a1f166a5135"
logdir="/var/log/cfapi"

app = Flask(__name__)

@app.route(f"/cfiran/{token}/<string:provider>/<string:result>", methods=["POST"])
def writeResult(provider, result):
    date = today.strftime("%b-%d-%Y-%H")
    with open(f"{logdir}/{provider}.{date}.log", "a") as resultFile:
        resultFile.write(result+"\n")
    return("Done")

if __name__ == "__main__":
    app.run(host='0.0.0.0',port='8452')

