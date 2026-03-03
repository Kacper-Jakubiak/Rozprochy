import socket

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    print(type(s))