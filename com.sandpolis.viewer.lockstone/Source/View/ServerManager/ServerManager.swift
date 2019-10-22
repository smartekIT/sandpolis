//****************************************************************************//
//                                                                            //
//                Copyright © 2015 - 2019 Subterranean Security               //
//                                                                            //
//  Licensed under the Apache License, Version 2.0 (the "License");           //
//  you may not use this file except in compliance with the License.          //
//  You may obtain a copy of the License at                                   //
//                                                                            //
//      http://www.apache.org/licenses/LICENSE-2.0                            //
//                                                                            //
//  Unless required by applicable law or agreed to in writing, software       //
//  distributed under the License is distributed on an "AS IS" BASIS,         //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  //
//  See the License for the specific language governing permissions and       //
//  limitations under the License.                                            //
//                                                                            //
//****************************************************************************//
import UIKit
import FirebaseAuth
import FirebaseFirestore
import NIOSSL

class ServerManager: UITableViewController {

	/// Firebase reference
	private let ref = Firestore.firestore().collection("/user/\(Auth.auth().currentUser!.uid)/server")

	private var servers = [SandpolisServer]()

	private var refListener: ListenerRegistration!

	override func viewDidLoad() {
		super.viewDidLoad()

		// Synchronize table data
		refListener = ref.addSnapshotListener({ querySnapshot, error in
			guard let servers = querySnapshot?.documents else {
				return
			}

			self.servers = servers.map { server -> SandpolisServer in
				return SandpolisServer(server)
			}
			self.tableView.reloadData()
			self.refreshServerStates()
			//self.refreshServerLocations()
		})
		
		// Setup refresh control
		refreshControl = UIRefreshControl()
		refreshControl?.addTarget(self, action: #selector(refreshTable), for: .valueChanged)
	}
	
	@objc func refreshTable() {
		// Spawn synchronous connection attempts
		DispatchQueue.global(qos: .utility).async {
			for server in self.servers {
				server.online = SandpolisUtil.testConnect(server.address, 10101)
			}
			DispatchQueue.main.async {
				self.tableView.reloadData()
				self.refreshControl?.endRefreshing()
			}
		}
	}

	/// Attempt to connect to each server in the list
	func refreshServerStates() {

		// Spawn concurrent connection attempts
		for server in servers {
			DispatchQueue.global(qos: .utility).async {
				server.online = SandpolisUtil.testConnect(server.address, 10101)
				DispatchQueue.main.async {
					self.tableView.reloadData()
				}
			}
		}
	}

	override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
		return servers.count
	}

	override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
		let server = servers[indexPath.row]
		let cell = tableView.dequeueReusableCell(withIdentifier: "ServerCell", for: indexPath) as! ServerCell
		cell.setContent(server)
		return cell
	}

	override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
		let server = servers[indexPath.row]
		if let online = server.online {
			if online {
				connectToServer(server: server)
			} else {
				server.online = nil
				self.tableView.reloadData()

				// Retry connection probe
				DispatchQueue.global(qos: .utility).async {
					server.online = SandpolisUtil.testConnect(server.address, 10101)
					DispatchQueue.main.async {
						self.tableView.reloadData()
					}
				}
			}
		}
	}

	override func tableView(_ tableView: UITableView, trailingSwipeActionsConfigurationForRowAt indexPath:
			IndexPath) -> UISwipeActionsConfiguration? {
		let delete = UIContextualAction(style: .destructive, title: "Delete") { action, view, completion in
			self.servers[indexPath.row].reference.delete()
			completion(true)
		}
		let edit = UIContextualAction(style: .normal, title: "Edit") { action, view, completion in
			self.performSegue(withIdentifier: "EditServerSegue", sender: indexPath)
			completion(true)
		}
		let config = UISwipeActionsConfiguration(actions: [delete, edit])
		config.performsFirstActionWithFullSwipe = false
		return config
	}

	override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
		if segue.identifier == "AddServerSegue",
			let addServerView = segue.destination as? AddServer {
			addServerView.serverReference = ref.document()
		} else if segue.identifier == "EditServerSegue",
			let addServerView = segue.destination as? AddServer {
			let indexPath = sender as! IndexPath
			addServerView.server = servers[indexPath.row]
			addServerView.serverReference = addServerView.server.reference
		} else if segue.identifier == "ShowHostSegue",
			let mainTab = segue.destination as? MainTabController {
			mainTab.server = sender as? SandpolisServer
		} else {
			fatalError("Unexpected segue: \(segue.identifier ?? "unknown")")
		}
	}

	func connectToServer(server: SandpolisServer) {
		let connection = SandpolisConnection(server.address, 10101)
		connection.connectionFuture.whenSuccess {
			SandpolisUtil.connection = connection
			self.loginToServer(server: server)
		}
		connection.connectionFuture.whenFailure { (error: Error) in
			if let sslError = error as? NIOSSLError {
				DispatchQueue.main.async {
					let alert = UIAlertController(title: "Continue connection?", message: "The server's certificate is invalid. If you continue, the connection is not guaranteed to be secure. To make a secure connection, install a valid certificate on the server.", preferredStyle: .alert)
					alert.addAction(UIAlertAction(title: "Continue", style: .destructive) { _ in
						let connection = SandpolisConnection(server.address, 10101, certificateVerification: .none)
						connection.connectionFuture.whenSuccess {
							SandpolisUtil.connection = connection
							self.loginToServer(server: server)
						}
						connection.connectionFuture.whenFailure { (error: Error) in
							print(error)
							self.onConnectFail(failureMessage: "Failed to connect to server.")
						}
					})
					alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))

					self.present(alert, animated: true)
				}
			} else {
				self.onConnectFail(failureMessage: "Failed to connect to server.")
			}
		}
	}

	func loginToServer(server: SandpolisServer) {
		let login = SandpolisUtil.connection.login(server.username, server.password)
		login.whenSuccess { rs in
			if rs.rsOutcome.result {
				DispatchQueue.main.async {
					self.performSegue(withIdentifier: "ShowHostSegue", sender: server)
				}
			} else {
				self.onConnectFail(failureMessage: "Failed to login to server.")
			}
		}
		login.whenFailure { (error: Error) in
			self.onConnectFail(failureMessage: "Failed to login to server.")
		}
	}

	func onConnectFail(failureMessage: String) {
		DispatchQueue.main.async {
			self.tableView.reloadData()
			let alert = UIAlertController(title: "Connection failure", message: failureMessage, preferredStyle: .alert)
			alert.addAction(UIAlertAction(title: "OK", style: .default))
			self.present(alert, animated: true, completion: nil)
		}
	}
}
