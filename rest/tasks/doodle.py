from fastapi import FastAPI, HTTPException, Response, status
from uuid import uuid4
from datetime import datetime
from pydantic import BaseModel

app = FastAPI()

@app.get("/")
async def root():
    return {"message": "Doodle Voting API"}


class PollModel(BaseModel):
    title: str
    createdBy: str
    options: list[str]
    description: str = ""

class VoteModel(BaseModel):
    voterName: str
    optionId: str

polls_database: list[dict] = []

def find_poll_db(poll_id: str) -> dict:
    global polls_database
    poll = next((p for p in polls_database if p["id"] == poll_id), None)
    if not poll:
        raise HTTPException(status_code=404, detail="PollModel not found")
    return poll

@app.post("/v1/polls", status_code=status.HTTP_201_CREATED)
async def create_poll(poll: PollModel):
    new_poll = {
        "id": str(uuid4()),
        "title": poll.title,
        "description": poll.description,
        "createdBy": poll.createdBy,
        "createdAt": datetime.now(),
        "options": {str(uuid4()): opt for opt in poll.options},
        "votes": {}
    }

    polls_database.append(new_poll)
    return new_poll


@app.get("/v2/polls")
async def get_polls(skip: int = 0, limit: int = 10):
    return polls_database[skip : skip + limit]


@app.get("/v3/polls/{poll_id}")
async def get_poll(poll_id: str):
    poll = find_poll_db(poll_id)
    return poll


@app.put("/v4/polls/{poll_id}")
async def update_poll(poll_id: str, poll_data: PollModel):
    poll = find_poll_db(poll_id)

    poll["title"] = poll_data.title
    poll["description"] = poll_data.description
    poll["createdBy"] = poll_data.createdBy
    poll["options"] = {str(uuid4()): opt for opt in poll.options}

    return poll


@app.delete("/v5/polls/{poll_id}")
async def delete_poll(poll_id: str):
    _ = find_poll_db(poll_id)

    global polls_database
    polls_database = [p for p in polls_database if p["id"] != poll_id]

    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.post("/v6/polls/{poll_id}/votes", status_code=status.HTTP_201_CREATED)
async def add_vote(poll_id: str, vote_data: VoteModel):
    poll = find_poll_db(poll_id)

    if vote_data.voterName in poll["votes"]:
      raise HTTPException(status_code=400, detail="User already voted")

    if vote_data.optionId not in poll["options"]:
      raise HTTPException(status_code=400, detail="Invalid option")

    poll["votes"][vote_data.voterName] = vote_data.optionId

    return {vote_data.voterName: vote_data.optionId}


@app.put("/v8/polls/{poll_id}/votes/{voter_name}")
async def update_vote(poll_id: str, voter_name: str, vote_data: VoteModel):
    poll = find_poll_db(poll_id)

    if voter_name not in poll["votes"]:
        raise HTTPException(status_code=404, detail="Vote not found")

    if vote_data.optionId not in poll["options"]:
      raise HTTPException(status_code=400, detail="Invalid option")


    poll["votes"][voter_name] = vote_data.optionId

    return {voter_name: vote_data.optionId}

@app.delete("/v4/polls/{poll_id}/votes/{voter_name}")
async def delete_vote(poll_id: str, voter_name: str):
    poll = find_poll_db(poll_id)

    if voter_name not in poll["votes"]:
        raise HTTPException(status_code=404, detail="Vote not found")

    del poll["votes"][voter_name]

    return Response(status_code=status.HTTP_204_NO_CONTENT)

@app.get("/v7/polls/{poll_id}/results")
async def get_results(poll_id: str):
    poll = find_poll_db(poll_id)

    results = []

    for option_id, option_text in poll["options"].items():
        count = sum(1 for v in poll["votes"].values() if v == option_id)

        results.append({
            "optionId": option_id,
            "option": option_text,
            "votes": count
        })

    return results