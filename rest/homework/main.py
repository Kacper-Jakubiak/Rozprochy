from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
import asyncio
import httpx

app = FastAPI()

URL = "https://api.triptocarbon.xyz/v1/footprint"

TRANSPORT_TO_MODES: dict[str, list[str]] = {
    'car': ['dieselCar', 'petrolCar', 'anyCar', 'motorbike'],
    'plane': ['economyFlight', 'businessFlight', 'firstclassFlight', 'anyFlight'],
    'public': ['taxi', 'bus', 'transitRail']
}

COUNTRY_CODES: list[str] = ['usa', 'gbr', 'def']

def get_footprint_dict(modes: list[str], footprints: list[float]) -> dict[str, float]:
    if not modes or not footprints or len(modes) != len(footprints):
        raise ValueError("Modes and footprints lists must be non-empty and of the same length.")
    
    mean = round(sum(footprints) / len(footprints), 2) if footprints else 0.0
    min_value = min(footprints)
    max_value = max(footprints)

    return  dict(zip(modes + ["mean", "min", "max"], footprints + [mean, min_value, max_value]))

def generate_html_answer(footprint_dict: dict[str, float], best_mode: str) -> str:
    html_content = f"""
    <html>
        <head>
            <title>Trip Carbon Footprint</title>
        </head>
        <body>
            <h1>Carbon Footprint Results (in kg CO2)</h1>
            <ul>
                {"".join(f"<li>{label}: {footprint}</li>" for label, footprint in footprint_dict.items())}
            </ul>
            <p>Lowest emission mode: <strong>{best_mode}</strong></p>
        </body>
    </html>
    """
    return html_content

async def fetch_footprint(client: httpx.AsyncClient, mode: str, distance: str, country: str) -> float:
    params = {
        "activity": distance,
        "activityType": 'miles',
        "mode": mode,
        "country": country
    }
    response = await client.get(URL, params=params, timeout=10)
    if response.status_code != 200:
        raise HTTPException(status_code=502, detail=f"Carbon API error ({response.status_code}): {response.text}")
    
    footprint = response.json().get("carbonFootprint")
    return float(footprint)

@app.get("/")
async def root_html():
    html_content = """
    <html>
    <head>
    <title>Flight Carbon Calculator</title>
    </head>
    <body>
        <form action="/analyze-trip" method="get">
            <div>
                <label>Distance (miles):</label><br>
                <input type="number" name="distance" step="50" required>
            </div>
            <br>
            <div>
                <label>Transport Type:</label><br>
                <select name="transport_type" required>
                    <option value="">Select a vehicle type</option>
                    <option value="car">Car</option>
                    <option value="plane">Plane</option>
                    <option value="public">Public Transport</option>
                </select>
            </div>
            <br>
            <div>
                <label>Country:</label><br>
                <select name="country" required>
                    <option value="">Select a country</option>
                    <option value="usa">United States</option>
                    <option value="gbr">Great Britain</option>
                    <option value="def">Other</option>
                </select>
            </div>
            <br>
            <input type="submit" value="Submit">
        </form>
    </body>
    </html>
    """

    return HTMLResponse(content=html_content, status_code=200)


@app.get("/analyze-trip")
async def analyze_trip(distance: float, transport_type: str, country: str):
    if transport_type not in TRANSPORT_TO_MODES.keys():
        raise HTTPException(status_code=400, detail=f"Invalid vehicle type. Must be one of: {', '.join(TRANSPORT_TO_MODES.keys())}")
    
    if country not in COUNTRY_CODES:
        raise HTTPException(status_code=400, detail=f"Invalid country code. Must be one of: {', '.join(COUNTRY_CODES)}")

    modes = TRANSPORT_TO_MODES[transport_type]

    async with httpx.AsyncClient() as client:
        tasks = [fetch_footprint(client, mode, distance, country) for mode in modes]
        footprints = await asyncio.gather(*tasks)

    footprint_dict = get_footprint_dict(modes, footprints)
    best_mode = modes[footprints.index(footprint_dict["min"])]

    return HTMLResponse(content=generate_html_answer(footprint_dict, best_mode), status_code=200)