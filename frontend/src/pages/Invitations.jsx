import { useEffect, useState } from 'react';

import {
  Users,
  Send,
  Clock3,
  CircleCheck,
  Building2,
  Search,
  SlidersHorizontal,
  RefreshCw,
  Trash2,
  ChevronLeft,
  ChevronRight,
  Info
} from 'lucide-react';

export default function Invitations() {

  const [organizations, setOrganizations] = useState([]);
  const [invitations, setInvitations] = useState([]);

  useEffect(() => {
    fetch('/api/organizations')
      .then(response => response.json())
      .then(data => {
        console.log('ORGANIZATIONS:', data);
        setOrganizations(data);
      })
      .catch(error => console.error(error));
  }, []);

  const rows = [
    {
      email: 'jean.dupont@example.com',
      organisation: 'TransCompany',
      description: 'Transports & Logistique',
      role: 'Administrateur',
      status: 'En attente',
      sent: '28 juil. 2026',
      expires: '04 août 2026'
    },
    {
      email: 'marie.martin@example.com',
      organisation: 'TransCompany',
      description: 'Transports & Logistique',
      role: 'Comptable',
      status: 'En attente',
      sent: '27 juil. 2026',
      expires: '03 août 2026'
    },
    {
      email: 'lucas.bernard@example.com',
      organisation: 'FacturX Partners',
      description: 'Services',
      role: 'Membre',
      status: 'Acceptée',
      sent: '25 juil. 2026',
      expires: '01 août 2026'
    },
    {
      email: 'sophie.durand@example.com',
      organisation: 'TransCompany',
      description: 'Transports & Logistique',
      role: 'Client',
      status: 'Expirée',
      sent: '20 juil. 2026',
      expires: '27 juil. 2026'
    },
    {
      email: 'paul.moreau@example.com',
      organisation: 'FacturX Partners',
      description: 'Services',
      role: 'Membre',
      status: 'Acceptée',
      sent: '18 juil. 2026',
      expires: '25 juil. 2026'
    }
  ];

  const statusStyle = (status) => {
    if (status === 'En attente') {
      return {
        background: '#fff7df',
        color: '#d78b13',
        border: '1px solid #fde6a8'
      };
    }

    if (status === 'Acceptée') {
      return {
        background: '#eafaf1',
        color: '#28a76a',
        border: '1px solid #c9f0db'
      };
    }

    return {
      background: '#fff0f0',
      color: '#ef5555',
      border: '1px solid #ffd1d1'
    };
  };

  return (
    <div style={{
      padding: '24px 28px',
      width: '100%',
      boxSizing: 'border-box'
    }}>

      {/* HEADER */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start'
      }}>

        <div>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
          }}>
            <Users size={18} />

            <h1 style={{
              margin: 0,
              fontSize: '20px',
              fontWeight: 700,
              color: '#111827'
            }}>
              Invitations
            </h1>
          </div>

          <p style={{
            margin: '7px 0 0',
            fontSize: '12px',
            color: '#6b7280'
          }}>
            Gérez les invitations envoyées à rejoindre vos organisations.
          </p>
        </div>

        <button style={{
          display: 'flex',
          alignItems: 'center',
          gap: '7px',
          padding: '9px 16px',
          background: '#152846',
          color: '#fff',
          border: 'none',
          borderRadius: '6px',
          fontSize: '12px',
          cursor: 'pointer'
        }}>
          <span style={{ fontSize: '18px' }}>+</span>
          Envoyer une invitation
        </button>

      </div>


      {/* STAT CARDS */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
        gap: '20px',
        marginTop: '28px'
      }}>

        {/* CARD 1 */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '18px',
          padding: '20px',
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '7px'
        }}>

          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '50%',
            background: '#edf6ff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Send size={19} color="#4a9eff" />
          </div>

          <div>
            <div style={{
              fontSize: '23px',
              fontWeight: 700
            }}>
              8
            </div>

            <div style={{ fontSize: '12px' }}>
              Invitations envoyées
            </div>

            <div style={{
              fontSize: '11px',
              color: '#8b93a1',
              marginTop: '4px'
            }}>
              Au total
            </div>
          </div>

        </div>


        {/* CARD 2 */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '18px',
          padding: '20px',
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '7px'
        }}>

          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '50%',
            background: '#fff6e8',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Clock3 size={19} color="#e8a32b" />
          </div>

          <div>
            <div style={{
              fontSize: '23px',
              fontWeight: 700
            }}>
              5
            </div>

            <div style={{ fontSize: '12px' }}>
              En attente
            </div>

            <div style={{
              fontSize: '11px',
              color: '#8b93a1',
              marginTop: '4px'
            }}>
              Invitations en cours
            </div>
          </div>

        </div>


        {/* CARD 3 */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '18px',
          padding: '20px',
          background: '#fff',
          border: '1px solid #e5e7eb',
          borderRadius: '7px'
        }}>

          <div style={{
            width: '42px',
            height: '42px',
            borderRadius: '50%',
            background: '#eafaf1',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <CircleCheck size={19} color="#2fb778" />
          </div>

          <div>
            <div style={{
              fontSize: '23px',
              fontWeight: 700
            }}>
              3
            </div>

            <div style={{ fontSize: '12px' }}>
              Acceptées
            </div>

            <div style={{
              fontSize: '11px',
              color: '#8b93a1',
              marginTop: '4px'
            }}>
              Invitations acceptées
            </div>
          </div>

        </div>

      </div>


      {/* TABLE CONTAINER */}
      <div style={{
        marginTop: '20px',
        padding: '0 14px 16px',
        background: '#fff',
        border: '1px solid #e5e7eb',
        borderRadius: '7px',
        width: '100%',
        boxSizing: 'border-box'
      }}>

        {/* TABS + SEARCH */}
        <div style={{
          minHeight: '52px',
          display: 'flex',
          alignItems: 'center',
          borderBottom: '1px solid #e5e7eb',
          gap: '24px'
        }}>

          {[
            'Toutes',
            'En attente',
            'Acceptées',
            'Expirées',
            'Révoquées'
          ].map((tab, index) => (
            <div
              key={tab}
              style={{
                height: '52px',
                display: 'flex',
                alignItems: 'center',
                fontSize: '11px',
                cursor: 'pointer',
                color: index === 0 ? '#4a8ff0' : '#6b7280',
                borderBottom: index === 0
                  ? '2px solid #79aef8'
                  : '2px solid transparent'
              }}
            >
              {tab}
            </div>
          ))}


          <div style={{
            marginLeft: 'auto',
            display: 'flex',
            gap: '8px'
          }}>

            <div style={{
              width: '270px',
              height: '34px',
              display: 'flex',
              alignItems: 'center',
              padding: '0 10px',
              border: '1px solid #e5e7eb',
              borderRadius: '5px',
              boxSizing: 'border-box'
            }}>

              <input
                type="text"
                placeholder="Rechercher par email ou organisation..."
                style={{
                  width: '100%',
                  border: 'none',
                  outline: 'none',
                  fontSize: '11px'
                }}
              />

              <Search size={14} color="#7b8493" />

            </div>

            <button style={{
              display: 'flex',
              alignItems: 'center',
              gap: '7px',
              height: '34px',
              padding: '0 12px',
              background: '#fff',
              border: '1px solid #e5e7eb',
              borderRadius: '5px',
              cursor: 'pointer',
              fontSize: '11px'
            }}>
              <SlidersHorizontal size={14} />
              Filtres
            </button>

          </div>

        </div>


        {/* TABLE */}
        <div style={{
          overflowX: 'auto',
          marginTop: '14px'
        }}>

          <table style={{
            width: '100%',
            borderCollapse: 'collapse',
            fontSize: '11px'
          }}>

            <thead>
              <tr style={{
                background: '#fafbfc',
                color: '#77808f'
              }}>
                {[
                  'Email',
                  'Organisation',
                  'Rôle',
                  'Statut',
                  'Envoyée le',
                  'Expire le',
                  'Actions'
                ].map(title => (
                  <th
                    key={title}
                    style={{
                      textAlign: 'left',
                      padding: '12px',
                      borderTop: '1px solid #e5e7eb',
                      borderBottom: '1px solid #e5e7eb',
                      fontWeight: 500
                    }}
                  >
                    {title}
                  </th>
                ))}
              </tr>
            </thead>


            <tbody>

              {rows.map((row, index) => (

                <tr key={index} style={{
                  borderBottom: '1px solid #edf0f3'
                }}>

                  <td style={{ padding: '14px 12px' }}>
                    {row.email}
                  </td>


                  <td style={{ padding: '14px 12px' }}>
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '9px'
                    }}>

                      <div style={{
                        width: '28px',
                        height: '28px',
                        background: '#edf5ff',
                        borderRadius: '5px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}>
                        <Building2
                          size={14}
                          color="#4a8ff0"
                        />
                      </div>

                      <div>
                        <div style={{
                          color: '#253044',
                          fontWeight: 500
                        }}>
                          {row.organisation}
                        </div>

                        <div style={{
                          fontSize: '10px',
                          color: '#8b93a1',
                          marginTop: '3px'
                        }}>
                          {row.description}
                        </div>
                      </div>

                    </div>
                  </td>


                  <td style={{ padding: '14px 12px' }}>
                    <span style={{
                      background: '#eef4ff',
                      color: '#4b77ba',
                      padding: '3px 6px',
                      borderRadius: '4px',
                      fontSize: '10px'
                    }}>
                      {row.role}
                    </span>
                  </td>


                  <td style={{ padding: '14px 12px' }}>
                    <span style={{
                      ...statusStyle(row.status),
                      padding: '3px 7px',
                      borderRadius: '4px',
                      fontSize: '10px'
                    }}>
                      {row.status}
                    </span>
                  </td>


                  <td style={{ padding: '14px 12px' }}>
                    {row.sent}
                  </td>


                  <td style={{ padding: '14px 12px' }}>
                    {row.expires}
                  </td>


                  <td style={{ padding: '14px 12px' }}>

                    {row.status === 'Acceptée' ? (
                      <span style={{ color: '#9ca3af' }}>
                        —
                      </span>
                    ) : (
                      <div style={{
                        display: 'flex',
                        gap: '7px'
                      }}>

                        <button style={{
                          width: '30px',
                          height: '30px',
                          background: '#fff',
                          border: '1px solid #e5e7eb',
                          borderRadius: '5px',
                          cursor: 'pointer'
                        }}>
                          <RefreshCw size={13} />
                        </button>

                        {row.status === 'En attente' && (
                          <button style={{
                            width: '30px',
                            height: '30px',
                            background: '#fff4f4',
                            color: '#ef5555',
                            border: '1px solid #ffe2e2',
                            borderRadius: '5px',
                            cursor: 'pointer'
                          }}>
                            <Trash2 size={13} />
                          </button>
                        )}

                      </div>
                    )}

                  </td>

                </tr>

              ))}

            </tbody>

          </table>

        </div>


        {/* PAGINATION */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginTop: '16px',
          fontSize: '11px',
          color: '#6b7280'
        }}>

          <span>
            1–5 sur 8 invitations
          </span>

          <div style={{
            display: 'flex',
            gap: '6px'
          }}>

            <button style={pageButton}>
              <ChevronLeft size={14} />
            </button>

            <button style={{
              ...pageButton,
              color: '#357de8',
              background: '#f4f8ff',
              borderColor: '#cfe0fb'
            }}>
              1
            </button>

            <button style={pageButton}>
              2
            </button>

            <button style={pageButton}>
              <ChevronRight size={14} />
            </button>

          </div>

        </div>

      </div>


      {/* INFORMATION */}
      <div style={{
        marginTop: '18px',
        padding: '15px 16px',
        background: '#eff7ff',
        border: '1px solid #dbeafe',
        borderRadius: '6px',
        width: '100%',
        boxSizing: 'border-box'
      }}>

        <div style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: '12px'
        }}>

          <Info size={16} color="#3985e6" />

          <div>

            <div style={{
              fontSize: '11px',
              fontWeight: 700,
              marginBottom: '4px'
            }}>
              Comment ça fonctionne ?
            </div>

            <div style={{
              fontSize: '11px',
              color: '#667085',
              lineHeight: '18px'
            }}>
              Envoyez une invitation à un utilisateur. Il recevra un
              email avec un lien sécurisé pour rejoindre votre organisation.
              <br />
              Le lien expire automatiquement à la date indiquée.
            </div>

          </div>

        </div>

      </div>

    </div>
  );
}


const pageButton = {
  width: '32px',
  height: '32px',
  background: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: '5px',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  cursor: 'pointer',
  fontSize: '11px'
};