import socket

HOST = "localhost"
PORT = 5000

def main():
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.connect((HOST, PORT))
            print("Connected to server.")

            while True:
                message = input("Enter message: ")

                if message.lower() == "quit":
                    print("Closing connection.")
                    break

                s.sendall((message + "\n").encode())

                data = s.recv(1024)
                if not data:
                    print("Server closed connection.")
                    break

                print("Server response:", data.decode().strip())

    except ConnectionRefusedError:
        print("Could not connect to server.")
    except Exception as e:
        print("Error:", e)


if __name__ == "__main__":
    main()