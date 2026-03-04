import socket;

serverIP = "127.0.0.1"
serverPort = 9003

msg_bytes = (300).to_bytes(4, byteorder='little')

print('PYTHON CLIENT 3')
client = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

client.sendto(msg_bytes, (serverIP, serverPort))

buff, _ = client.recvfrom(4)

response_int = int.from_bytes(buff, byteorder='little')

print("Received from server:", response_int)

client.close()