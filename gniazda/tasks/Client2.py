import socket;

serverIP = "127.0.0.1"
serverPort = 9002
msg = "żółta gęś"

print('PYTHON CLIENT 2')
client = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
client.sendto(bytes(msg, 'utf-8'), (serverIP, serverPort))

client.close()